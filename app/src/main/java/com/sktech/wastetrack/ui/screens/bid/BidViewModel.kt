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
            val user = authRepository.getCurrentUser()
            _userRole.value = user?.role
            val factoryId = user?.factoryId?.ifBlank { Constants.DEFAULT_FACTORY_ID } ?: Constants.DEFAULT_FACTORY_ID

            launch {
                bidRepository.getActiveBidRequests().collect { requests ->
                    _state.update { it.copy(bidRequests = requests) }
                }
            }
            launch {
                scrapEntryDao.getByFactory(factoryId).collect { entries ->
                    _state.update { it.copy(scrapEntries = entries) }
                }
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
                val user = authRepository.getCurrentUser()
                val factoryId = user?.factoryId?.ifBlank { Constants.DEFAULT_FACTORY_ID } ?: Constants.DEFAULT_FACTORY_ID
                val userId = user?.id ?: "supervisor-001"

                val requestId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val request = BidRequest(
                    id = requestId,
                    factoryId = factoryId,
                    createdByUserId = userId,
                    scrapEntryId = scrapEntry.id,
                    scrapCategory = try { com.sktech.wastetrack.domain.model.ScrapCategory.valueOf(scrapEntry.category) } catch (e: Exception) { com.sktech.wastetrack.domain.model.ScrapCategory.OTHER },
                    estimatedWeightKg = scrapEntry.weightKg,
                    reservePricePerKg = reserve,
                    auctionStartTime = now,
                    auctionEndTime = now + Constants.AUCTION_DURATION_HOURS * 60 * 60 * 1000,
                    status = com.sktech.wastetrack.domain.model.BidStatus.OPEN
                )
                
                // Optimistic UI state update: instant insertion into state
                _state.update {
                    it.copy(
                        bidRequests = listOf(request) + it.bidRequests.filter { r -> r.id != request.id },
                        isCreating = false,
                        showCreateDialog = false,
                        selectedScrapEntryId = null,
                        reservePrice = "",
                        successMessage = "Bid request created! Recyclers notified."
                    )
                }

                bidRepository.createBidRequest(request)
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
                val recyclerId = user?.id ?: "recycler-001"
                val recyclerName = user?.organizationName?.ifBlank { user.name } ?: "Certified Recycler"
                val bid = com.sktech.wastetrack.domain.model.Bid(
                    id = UUID.randomUUID().toString(),
                    bidRequestId = request.id,
                    recyclerId = recyclerId,
                    recyclerName = recyclerName,
                    pricePerKg = pricePerKg,
                    totalBidAmount = pricePerKg * request.estimatedWeightKg,
                    isWinning = false,
                    submittedAt = System.currentTimeMillis()
                )

                // Optimistic UI update: instantly show bid in state
                _detailState.update { current ->
                    val updated = (listOf(bid) + current.bids.filter { it.id != bid.id })
                        .sortedByDescending { it.pricePerKg }
                    current.copy(bids = updated)
                }

                bidRepository.submitBid(bid)
                _state.update { it.copy(successMessage = "Bid placed successfully!") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun awardBid(bidId: String, requestId: String = _detailState.value.bidRequest?.id ?: "") {
        val request = _detailState.value.bidRequest ?: return
        val targetRequestId = if (requestId.isNotBlank()) requestId else request.id
        viewModelScope.launch {
            try {
                bidRepository.awardBid(bidId, targetRequestId)
                
                // Optimistic Local Update
                _detailState.update { current ->
                    current.copy(
                        bidRequest = current.bidRequest?.copy(status = com.sktech.wastetrack.domain.model.BidStatus.AWARDED),
                        bids = current.bids.map { bid ->
                            if (bid.id == bidId) bid.copy(isWinning = true) else bid.copy(isWinning = false)
                        }
                    )
                }

                // Also insert into local Room DB for offline traceability
                scrapEntryDao.getById(request.scrapEntryId)?.let { entry ->
                    val winningBid = _detailState.value.bids.find { it.id == bidId }
                    val bidRequestEntity = com.sktech.wastetrack.data.local.db.entity.BidRequestEntity(
                        id = request.id,
                        factoryId = request.factoryId,
                        scrapEntryId = request.scrapEntryId,
                        scrapCategory = request.scrapCategory.name,
                        estimatedWeightKg = request.estimatedWeightKg,
                        reservePricePerKg = request.reservePricePerKg,
                        auctionStartTime = request.auctionStartTime,
                        auctionEndTime = request.auctionEndTime,
                        status = "AWARDED"
                    )
                    
                    // Enqueue sync queue so other nodes receive it
                    syncQueueDao.enqueue(
                        com.sktech.wastetrack.data.local.db.entity.SyncQueueEntity(
                            entityType = "BID_REQUEST",
                            entityId = request.id,
                            action = "AWARD",
                            payload = Gson().toJson(bidRequestEntity)
                        )
                    )
                }

                _state.update { it.copy(successMessage = "Bid awarded successfully!") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearSuccess() {
        _state.update { it.copy(successMessage = null) }
    }
}
