package com.sktech.wastetrack.ui.screens.bid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.domain.model.BidRequest
import com.sktech.wastetrack.domain.model.BidStatus
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.domain.repository.IBidRepository
import com.sktech.wastetrack.data.local.db.dao.BidDao
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
import com.sktech.wastetrack.data.local.db.dao.TransferDao
import com.sktech.wastetrack.data.local.db.entity.BidRequestEntity
import com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity
import com.sktech.wastetrack.data.local.db.entity.SyncQueueEntity
import com.sktech.wastetrack.data.local.db.entity.TransferEntity
import com.sktech.wastetrack.util.Constants
import com.sktech.wastetrack.util.HashUtils
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
    private val transferDao: TransferDao,
    private val bidDao: BidDao,
    private val syncQueueDao: SyncQueueDao,
    private val cloudSyncEngine: com.sktech.wastetrack.data.sync.CloudSyncEngine
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

            bidRepository.getAllBidRequests().collect { requests ->
                _state.update { it.copy(bidRequests = requests) }
            }
        }
        loadUnauctionedScrap()
    }

    private fun loadUnauctionedScrap() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            val factoryId = user?.factoryId?.ifBlank { Constants.DEFAULT_FACTORY_ID } ?: Constants.DEFAULT_FACTORY_ID
            scrapEntryDao.getByFactory(factoryId).collect { entries ->
                _state.update { it.copy(scrapEntries = entries) }
            }
        }
    }

    fun showCreateDialog() {
        _state.update { it.copy(showCreateDialog = true, error = null) }
    }

    fun openCreateDialog() = showCreateDialog()

    fun dismissCreateDialog() {
        _state.update {
            it.copy(
                showCreateDialog = false,
                selectedScrapEntryId = null,
                reservePrice = "",
                suggestedPrice = null,
                error = null
            )
        }
    }

    fun closeCreateDialog() = dismissCreateDialog()

    fun onScrapEntrySelected(entryId: String) {
        val entry = _state.value.scrapEntries.find { it.id == entryId }
        val category = entry?.category?.let {
            runCatching { com.sktech.wastetrack.domain.model.ScrapCategory.valueOf(it) }.getOrNull()
        } ?: com.sktech.wastetrack.domain.model.ScrapCategory.OTHER
        val suggested = marketRates[category] ?: 20f
        _state.update {
            it.copy(
                selectedScrapEntryId = entryId,
                suggestedPrice = suggested,
                reservePrice = String.format("%.0f", suggested)
            )
        }
    }

    fun selectScrapEntry(entryId: String) = onScrapEntrySelected(entryId)

    fun onReservePriceChanged(price: String) {
        _state.update { it.copy(reservePrice = price) }
    }

    fun createBidRequest() {
        val entryId = _state.value.selectedScrapEntryId
        if (entryId == null) {
            _state.update { it.copy(error = "Select a scrap entry to auction") }
            return
        }
        val reserve = _state.value.reservePrice.toFloatOrNull()
        if (reserve == null || reserve <= 0f) {
            _state.update { it.copy(error = "Enter a valid reserve price per kg") }
            return
        }
        val entry = _state.value.scrapEntries.find { it.id == entryId } ?: return
        val category = runCatching {
            com.sktech.wastetrack.domain.model.ScrapCategory.valueOf(entry.category)
        }.getOrDefault(com.sktech.wastetrack.domain.model.ScrapCategory.OTHER)

        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null) }
            try {
                val now = System.currentTimeMillis()
                val user = authRepository.getCurrentUser()
                val factoryId = user?.factoryId?.ifBlank { Constants.DEFAULT_FACTORY_ID } ?: Constants.DEFAULT_FACTORY_ID
                val request = BidRequest(
                    id = UUID.randomUUID().toString(),
                    scrapEntryId = entryId,
                    factoryId = factoryId,
                    scrapCategory = category,
                    estimatedWeightKg = entry.weightKg,
                    reservePricePerKg = reserve,
                    auctionStartTime = now,
                    auctionEndTime = now + Constants.AUCTION_DURATION_HOURS * 3600_000L,
                    status = BidStatus.OPEN
                )

                _state.update {
                    it.copy(
                        bidRequests = listOf(request) + it.bidRequests,
                        showCreateDialog = false,
                        selectedScrapEntryId = null,
                        reservePrice = "",
                        suggestedPrice = null,
                        isCreating = false,
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
                
                val winningBid = _detailState.value.bids.find { it.id == bidId }
                val recyclerId = winningBid?.recyclerId ?: "recycler-001"
                val user = authRepository.getCurrentUser()
                val supervisorId = user?.id ?: "supervisor-001"
                val now = System.currentTimeMillis()

                // 1. Optimistic Local Detail State Update
                _detailState.update { current ->
                    current.copy(
                        bidRequest = current.bidRequest?.copy(status = BidStatus.AWARDED),
                        bids = current.bids.map { bid ->
                            if (bid.id == bidId) bid.copy(isWinning = true) else bid.copy(isWinning = false)
                        }
                    )
                }

                // 2. Automatically spawn Transfer in Room DB for live Logistics/Fleet pipeline
                val transferId = UUID.randomUUID().toString()
                val contentHash = HashUtils.hashTransfer(
                    id = transferId,
                    scrapEntryId = request.scrapEntryId,
                    weightAtSource = request.estimatedWeightKg,
                    supervisorId = supervisorId,
                    timestamp = now
                )
                val transfer = TransferEntity(
                    id = transferId,
                    scrapEntryId = request.scrapEntryId,
                    fromFactoryId = request.factoryId,
                    toRecyclerId = recyclerId,
                    supervisorId = supervisorId,
                    weightAtSource = request.estimatedWeightKg,
                    vehicleNumber = "MH-15-TR-${(1000..9999).random()}",
                    status = "IN_TRANSIT",
                    syncStatus = "PENDING",
                    contentHash = contentHash,
                    initiatedAt = now
                )
                transferDao.insert(transfer)
                cloudSyncEngine.pushTransfer(transfer)

                // 3. Update BidRequestEntity in Room
                val bidRequestEntity = BidRequestEntity(
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
                bidDao.insertRequest(bidRequestEntity)

                // 4. Enqueue sync queue
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        entityType = "TRANSFER",
                        entityId = transferId,
                        action = "CREATE",
                        payload = Gson().toJson(transfer)
                    )
                )

                _state.update { it.copy(successMessage = "Auction awarded to ${winningBid?.recyclerName ?: "winning bidder"}! Transfer dispatched.") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearSuccess() {
        _state.update { it.copy(successMessage = null) }
    }
}
