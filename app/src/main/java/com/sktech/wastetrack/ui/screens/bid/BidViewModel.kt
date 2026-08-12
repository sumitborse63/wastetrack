package com.sktech.wastetrack.ui.screens.bid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.BidDao
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
import com.sktech.wastetrack.data.local.db.entity.BidEntity
import com.sktech.wastetrack.data.local.db.entity.BidRequestEntity
import com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity
import com.sktech.wastetrack.data.local.db.entity.SyncQueueEntity
import com.sktech.wastetrack.util.Constants
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BidMarketState(
    val bidRequests: List<BidRequestEntity> = emptyList(),
    val scrapEntries: List<ScrapEntryEntity> = emptyList(),
    val showCreateDialog: Boolean = false,
    val selectedScrapEntryId: String? = null,
    val reservePrice: String = "",
    val isCreating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

data class BidDetailState(
    val bidRequest: BidRequestEntity? = null,
    val bids: List<BidEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class BidViewModel @Inject constructor(
    private val bidDao: BidDao,
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
            bidDao.getRequestsByFactory(factoryId).collect { requests ->
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
                val request = BidRequestEntity(
                    id = requestId,
                    factoryId = factoryId,
                    scrapEntryId = scrapEntry.id,
                    scrapCategory = scrapEntry.category,
                    estimatedWeightKg = scrapEntry.weightKg,
                    reservePricePerKg = reserve,
                    auctionStartTime = now,
                    auctionEndTime = now + Constants.AUCTION_DURATION_HOURS * 60 * 60 * 1000,
                    status = "OPEN"
                )
                bidDao.insertRequest(request)

                // Simulate 2-4 recycler bids arriving
                generateSimulatedBids(requestId, scrapEntry.weightKg, reserve)

                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        entityType = "BID_REQUEST",
                        entityId = requestId,
                        action = "CREATE",
                        payload = Gson().toJson(request)
                    )
                )

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

    private suspend fun generateSimulatedBids(requestId: String, weightKg: Float, reservePrice: Float) {
        val recyclers = listOf(
            "Green Metals Nashik" to "rec-001",
            "EcoPlast Recyclers" to "rec-002",
            "Ambad Scrap Traders" to "rec-003",
            "Sinnar Recycle Hub" to "rec-004"
        )
        val bidCount = (2..4).random()
        recyclers.shuffled().take(bidCount).forEach { (name, id) ->
            val priceVariance = (0.8f + Math.random().toFloat() * 0.6f) // 80%-140% of reserve
            val price = reservePrice * priceVariance
            bidDao.insertBid(
                BidEntity(
                    id = UUID.randomUUID().toString(),
                    bidRequestId = requestId,
                    recyclerId = id,
                    recyclerName = name,
                    pricePerKg = price,
                    totalBidAmount = price * weightKg,
                    isWinning = false,
                    submittedAt = System.currentTimeMillis() + (1..60).random() * 60 * 1000L
                )
            )
        }
        // Mark highest bid as winning
        val bidsFlow = bidDao.getBidsByRequest(requestId)
        bidsFlow.first().maxByOrNull { it.pricePerKg }?.let {
            bidDao.markWinning(it.id)
        }
    }

    fun loadBidDetail(requestId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true) }
            val request = bidDao.getRequestById(requestId)
            _detailState.update { it.copy(bidRequest = request, isLoading = false) }
        }
        viewModelScope.launch {
            bidDao.getBidsByRequest(requestId).collect { bids ->
                _detailState.update { it.copy(bids = bids) }
            }
        }
    }

    fun awardBid(bidId: String, requestId: String) {
        viewModelScope.launch {
            bidDao.markWinning(bidId)
            val request = bidDao.getRequestById(requestId)
            if (request != null) {
                bidDao.updateRequest(request.copy(status = "AWARDED"))
            }
        }
    }

    fun clearSuccess() {
        _state.update { it.copy(successMessage = null) }
    }
}
