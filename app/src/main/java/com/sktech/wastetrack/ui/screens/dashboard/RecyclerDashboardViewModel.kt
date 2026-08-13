package com.sktech.wastetrack.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.TransferDao
import com.sktech.wastetrack.data.local.db.dao.BidDao
import com.sktech.wastetrack.data.local.db.dao.CertificateDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
import com.sktech.wastetrack.data.local.db.entity.TransferEntity
import com.sktech.wastetrack.data.local.db.entity.BidRequestEntity
import com.sktech.wastetrack.data.local.db.entity.CertificateEntity
import com.sktech.wastetrack.data.local.db.entity.SyncQueueEntity
import com.sktech.wastetrack.util.HashUtils
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RecyclerDashboardState(
    val activeBidsCount: Int = 0,
    val wonBidsCount: Int = 0,
    val totalWeightRecycledKg: Float = 0f,
    val certificateCount: Int = 0,
    val incomingShipments: List<TransferEntity> = emptyList(),
    val wonAuctions: List<BidRequestEntity> = emptyList(),
    val isOnline: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class RecyclerDashboardViewModel @Inject constructor(
    private val transferDao: TransferDao,
    private val bidDao: BidDao,
    private val certificateDao: CertificateDao,
    private val syncQueueDao: SyncQueueDao
) : ViewModel() {

    private val recyclerId = "mock_recycler_id"

    private val _state = MutableStateFlow(RecyclerDashboardState())
    val state: StateFlow<RecyclerDashboardState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Collect recycler-specific transfers
        viewModelScope.launch {
            transferDao.getByRecycler(recyclerId).collect { transfers ->
                val incoming = transfers.filter { it.status == "IN_TRANSIT" || it.status == "DELIVERED" }
                _state.update { it.copy(incomingShipments = incoming) }
            }
        }

        // Sum recycled weight
        viewModelScope.launch {
            transferDao.getRecycledWeightSum(recyclerId).collect { sum ->
                _state.update { it.copy(totalWeightRecycledKg = sum ?: 0f) }
            }
        }

        // Get count of verified transfers (which correspond to certifications)
        viewModelScope.launch {
            transferDao.getCountByRecyclerAndStatus(recyclerId, "VERIFIED").collect { count ->
                _state.update { it.copy(certificateCount = count) }
            }
        }

        // Fetch won auctions
        viewModelScope.launch {
            bidDao.getRequestsByFactory(com.sktech.wastetrack.util.Constants.DEFAULT_FACTORY_ID).collect { requests ->
                val won = requests.filter { it.status == "AWARDED" }
                _state.update { it.copy(wonAuctions = won, wonBidsCount = won.size) }
            }
        }
    }

    fun initiatePickup(requestId: String, vehicleNumber: String) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val transferId = UUID.randomUUID().toString()
                
                val request = bidDao.getRequestById(requestId) ?: return@launch
                
                val contentHash = HashUtils.hashTransfer(
                    id = transferId,
                    scrapEntryId = request.scrapEntryId,
                    weightAtSource = request.estimatedWeightKg,
                    supervisorId = "pilot-user-001",
                    timestamp = now
                )

                val transfer = TransferEntity(
                    id = transferId,
                    scrapEntryId = request.scrapEntryId,
                    fromFactoryId = request.factoryId,
                    toRecyclerId = recyclerId,
                    supervisorId = "pilot-user-001",
                    weightAtSource = request.estimatedWeightKg,
                    vehicleNumber = vehicleNumber,
                    status = "IN_TRANSIT",
                    syncStatus = "PENDING",
                    contentHash = contentHash,
                    initiatedAt = now
                )
                
                transferDao.insert(transfer)

                // Enqueue sync queue
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        entityType = "TRANSFER",
                        entityId = transferId,
                        action = "CREATE",
                        payload = Gson().toJson(transfer)
                    )
                )

                // Update BidRequest status to CLOSED
                val updatedRequest = request.copy(status = "CLOSED")
                bidDao.updateRequest(updatedRequest)

                _state.update { it.copy(successMessage = "Pickup initiated! Vehicle $vehicleNumber in transit.") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun verifyReceivedWeight(transferId: String, receivedWeight: Float) {
        viewModelScope.launch {
            try {
                val transfer = transferDao.getById(transferId) ?: return@launch
                val sourceWeight = transfer.weightAtSource
                val discrepancy = Math.abs(receivedWeight - sourceWeight)
                val tolerance = sourceWeight * 0.10f // 10% discrepancy tolerance
                
                val isDisputed = discrepancy > tolerance
                val finalStatus = if (isDisputed) "DISPUTED" else "VERIFIED"
                
                val updatedTransfer = transfer.copy(
                    weightAtDestination = receivedWeight,
                    weightDiscrepancy = discrepancy,
                    status = finalStatus,
                    completedAt = System.currentTimeMillis()
                )
                transferDao.insert(updatedTransfer)

                // Enqueue sync queue
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        entityType = "TRANSFER",
                        entityId = transferId,
                        action = "UPDATE",
                        payload = Gson().toJson(updatedTransfer)
                    )
                )

                if (finalStatus == "VERIFIED") {
                    // Generate MPCB certificate automatically
                    val certId = UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
                    val payload = mapOf(
                        "certificateId" to certId,
                        "type" to "MPCB_DISPOSAL",
                        "factoryId" to transfer.fromFactoryId,
                        "factoryName" to "Ambad MIDC Pilot Unit",
                        "recyclerId" to recyclerId,
                        "recyclerName" to "Mumbai Green Recyclers",
                        "transferId" to transfer.id,
                        "weightDisposedKg" to receivedWeight,
                        "disposalDate" to now,
                        "verificationHash" to transfer.contentHash
                    )
                    val jsonPayload = Gson().toJson(payload)
                    val signature = HashUtils.sha256(jsonPayload)

                    val certificate = CertificateEntity(
                        id = certId,
                        transferId = transfer.id,
                        factoryId = transfer.fromFactoryId,
                        type = "MPCB_DISPOSAL",
                        jsonPayload = jsonPayload,
                        digitalSignature = signature,
                        status = "GENERATED",
                        generatedAt = now
                    )
                    certificateDao.insert(certificate)
                    
                    _state.update { it.copy(successMessage = "Weight verified successfully! MPCB certificate generated.") }
                } else {
                    _state.update { it.copy(error = "AI ALERT: Weight discrepancy (Source: $sourceWeight kg, Recycler: $receivedWeight kg) exceeds 10% tolerance! Flagged as DISPUTED.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, successMessage = null) }
    }
}
