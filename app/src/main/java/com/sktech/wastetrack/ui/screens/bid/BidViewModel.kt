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

    private val _state = MutableStateFlow(BidMarketState())
    val state: StateFlow<BidMarketState> = _state.asStateFlow()

    private val _detailState = MutableStateFlow(BidDetailState())
    val detailState: StateFlow<BidDetailState> = _detailState.asStateFlow()

    init {
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
        _state.update { it.copy(showCreateDialog = false, selectedScrapEntryId = null, reservePrice = "") }
    }

    fun onScrapEntrySelected(id: String) {
        _state.update { it.copy(selectedScrapEntryId = id) }
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
            // Fetch the specific request from state for now
            val request = _state.value.bidRequests.find { it.id == requestId }
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

                val bid = com.sktech.wastetrack.domain.model.Bid(
                    id = UUID.randomUUID().toString(),
                    bidRequestId = request.id,
                    recyclerId = user.id,
                    recyclerName = user.name.takeIf { it.isNotBlank() } ?: "Recycler",
                    pricePerKg = pricePerKg,
                    totalBidAmount = pricePerKg * request.estimatedWeightKg
                )
                bidRepository.submitBid(bid)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun awardBid(bidId: String, requestId: String) {
        viewModelScope.launch {
            try {
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
