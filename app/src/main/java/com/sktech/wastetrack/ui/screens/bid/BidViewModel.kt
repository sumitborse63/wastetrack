package com.sktech.wastetrack.ui.screens.bid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.domain.model.BidRequest
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.domain.repository.IBidRepository
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
import com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity
import com.sktech.wastetrack.util.Constants
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BidMarketState(
    val bidRequests: List<BidRequest> = emptyList(),
    val scrapEntries: List<ScrapEntryEntity> = emptyList(),
    val showCreateDialog: Boolean = false,
    val selectedScrapEntryId: String? = null,
    val reservePrice: String = "",
    val suggestedPrice: Float? = null,
    val isCreating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

data class BidDetailState(
    val bidRequest: BidRequest? = null,
    val bids: List<com.sktech.wastetrack.domain.model.Bid> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class BidViewModel @Inject constructor(
    private val bidRepository: IBidRepository,
    private val authRepository: IAuthRepository,
    private val scrapEntryDao: ScrapEntryDao,
    private val syncQueueDao: SyncQueueDao
) : ViewModel() {

    private val factoryId = Constants.DEFAULT_FACTORY_ID

    private val marketRates = mapOf(
        com.sktech.wastetrack.domain.model.ScrapCategory.METAL to 45f,
        com.sktech.wastetrack.domain.model.ScrapCategory.PLASTIC to 25f,
        com.sktech.wastetrack.domain.model.ScrapCategory.PAPER to 15f,
        com.sktech.wastetrack.domain.model.ScrapCategory.EWASTE to 120f,
        com.sktech.wastetrack.domain.model.ScrapCategory.GLASS to 10f,
        com.sktech.wastetrack.domain.model.ScrapCategory.RUBBER to 12f,
        com.sktech.wastetrack.domain.model.ScrapCategory.CHEMICAL to 80f,
        com.sktech.wastetrack.domain.model.ScrapCategory.WOOD to 8f,
        com.sktech.wastetrack.domain.model.ScrapCategory.OTHER to 20f
    )

    private val _state = MutableStateFlow(BidMarketState())
    val state: StateFlow<BidMarketState> = _state.asStateFlow()

    private val _detailState = MutableStateFlow(BidDetailState())
    val detailState: StateFlow<BidDetailState> = _detailState.asStateFlow()

    private val _userRole = MutableStateFlow<com.sktech.wastetrack.domain.model.UserRole?>(null)
    val userRole = _userRole.asStateFlow()

    init {
        viewModelScope.launch {
            _userRole.value = authRepository.getCurrentUser()?.role
        }
        viewModelScope.launch {
            bidRepository.getActiveBidRequests().collect { requests ->
                _state.update { it.copy(bidRequests = requests) }
            }
        }
        viewModelScope.launch {
            scrapEntryDao.getByFactory(factoryId).collect { entries ->
                _state.update { it.copy(scrapEntries = entries) }
            }
        }
    }

    fun showCreateDialog() {
        _state.update { it.copy(showCreateDialog = true, error = null, successMessage = null) }
    }

    fun dismissCreateDialog() {
        _state.update { it.copy(showCreateDialog = false, selectedScrapEntryId = null, reservePrice = "", suggestedPrice = null) }
    }

    fun onScrapEntrySelected(id: String) {
        val entry = _state.value.scrapEntries.find { it.id == id }
        val suggested = if (entry != null) {
            val cat = try { com.sktech.wastetrack.domain.model.ScrapCategory.valueOf(entry.category) } catch (e: Exception) { com.sktech.wastetrack.domain.model.ScrapCategory.OTHER }
            marketRates[cat]
        } else null
        
        _state.update { it.copy(selectedScrapEntryId = id, suggestedPrice = suggested) }
    }

    fun onReservePriceChanged(price: String) {
        _state.update { it.copy(reservePrice = price) }
    }

    fun createBidRequest() {
        val current = _state.value
        val scrapEntry = current.scrapEntries.find { it.id == current.selectedScrapEntryId }
        if (scrapEntry == null) {
            _state.update { it.copy(error = "Please select a scrap entry") }
            return
        }
        val reserve = current.reservePrice.toFloatOrNull()
        if (reserve == null || reserve <= 0) {
            _state.update { it.copy(error = "Enter a valid reserve price per kg") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCreating = true) }
            try {
                val requestId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val request = BidRequest(
                    id = requestId,
                    factoryId = factoryId,
                    scrapEntryId = scrapEntry.id,
                    scrapCategory = try { com.sktech.wastetrack.domain.model.ScrapCategory.valueOf(scrapEntry.category) } catch (e: Exception) { com.sktech.wastetrack.domain.model.ScrapCategory.OTHER },
                    estimatedWeightKg = scrapEntry.weightKg,
                    reservePricePerKg = reserve,
                    auctionStartTime = now,
                    auctionEndTime = now + Constants.AUCTION_DURATION_HOURS * 60 * 60 * 1000,
                    status = com.sktech.wastetrack.domain.model.BidStatus.OPEN
                )
                bidRepository.createBidRequest(request)

                _state.update {
                    it.copy(
                        isCreating = false,
                        showCreateDialog = false,
                        selectedScrapEntryId = null,
                        reservePrice = "",
                        successMessage = "Bid request created! Recyclers notified."
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isCreating = false, error = e.message) }
            }
        }
    }

    fun loadBidDetail(requestId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true) }
            // Try in-memory first, then fall back to repository
            val request = _state.value.bidRequests.find { it.id == requestId }
                ?: bidRepository.getBidRequestById(requestId)
            _detailState.update { it.copy(bidRequest = request, isLoading = false) }
        }
        viewModelScope.launch {
            bidRepository.getBidsForRequest(requestId).collect { bids ->
                _detailState.update { it.copy(bids = bids) }
            }
        }
    }

    fun submitBid(pricePerKg: Float) {
        val request = _detailState.value.bidRequest ?: return
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                if (user == null) {
                    _state.update { it.copy(error = "User not logged in") }
                    return@launch
                }

                // Check if user already has a bid, update it instead of creating new if so
                val existingBid = _detailState.value.bids.find { it.recyclerId == user.id }
                
                val bid = com.sktech.wastetrack.domain.model.Bid(
                    id = existingBid?.id ?: UUID.randomUUID().toString(),
                    bidRequestId = request.id,
                    recyclerId = user.id,
                    recyclerName = user.name.takeIf { it.isNotBlank() } ?: "Recycler",
                    pricePerKg = pricePerKg,
                    totalBidAmount = pricePerKg * request.estimatedWeightKg
                )

                // Optimistic UI Update
                val currentBids = _detailState.value.bids.toMutableList()
                val index = currentBids.indexOfFirst { it.recyclerId == user.id }
                if (index != -1) {
                    currentBids[index] = bid
                } else {
                    currentBids.add(0, bid)
                }
                _detailState.update { it.copy(bids = currentBids) }

                bidRepository.submitBid(bid)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun awardBid(bidId: String, requestId: String) {
        viewModelScope.launch {
            try {
                // Optimistic update
                _detailState.update { current ->
                    val updatedBids = current.bids.map { bid ->
                        if (bid.id == bidId) bid.copy(isWinning = true) else bid
                    }
                    val updatedRequest = current.bidRequest?.copy(status = com.sktech.wastetrack.domain.model.BidStatus.AWARDED)
                    current.copy(bids = updatedBids, bidRequest = updatedRequest)
                }
                
                bidRepository.awardBid(bidId, requestId)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearSuccess() {
        _state.update { it.copy(successMessage = null) }
    }
}
