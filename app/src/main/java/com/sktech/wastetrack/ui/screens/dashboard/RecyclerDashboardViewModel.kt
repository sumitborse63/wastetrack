package com.sktech.wastetrack.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.BidDao
import com.sktech.wastetrack.data.local.db.dao.CertificateDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
import com.sktech.wastetrack.data.local.db.dao.TransferDao
import com.sktech.wastetrack.data.local.db.entity.BidRequestEntity
import com.sktech.wastetrack.data.local.db.entity.CertificateEntity
import com.sktech.wastetrack.data.local.db.entity.SyncQueueEntity
import com.sktech.wastetrack.data.local.db.entity.TransferEntity
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.util.Constants
import com.sktech.wastetrack.util.HashUtils
import com.sktech.wastetrack.domain.model.User
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
    val currentUser: User? = null,
    val activeBidsCount: Int = 0,
    val wonBidsCount: Int = 0,
    val totalWeightRecycledKg: Float = 0f,
    val certificateCount: Int = 0,
    val incomingShipments: List<TransferEntity> = emptyList(),
    val wonAuctions: List<BidRequestEntity> = emptyList(),
    val openAuctions: List<BidRequestEntity> = emptyList(),
    val isOnline: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class RecyclerDashboardViewModel @Inject constructor(
    private val transferDao: TransferDao,
    private val bidDao: BidDao,
    private val certificateDao: CertificateDao,
    private val syncQueueDao: SyncQueueDao,
    private val authRepository: IAuthRepository,
    private val cloudSyncEngine: com.sktech.wastetrack.data.sync.CloudSyncEngine
) : ViewModel() {

    private var currentUser: User? = null

    private val _state = MutableStateFlow(RecyclerDashboardState())
    val state: StateFlow<RecyclerDashboardState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            currentUser = user
            _state.update { it.copy(currentUser = user) }
            if (user == null || user.role != com.sktech.wastetrack.domain.model.UserRole.RECYCLER) {
                _state.update { it.copy(error = "Recycler account required") }
                return@launch
            }
            currentUser = user
            transferDao.getByRecycler(user.id).collect { transfers ->
                val incoming = transfers.filter { it.status == "IN_TRANSIT" || it.status == "DELIVERED" }
                _state.update { it.copy(incomingShipments = incoming) }
            }
        }

        // Sum recycled weight
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            transferDao.getRecycledWeightSum(user.id).collect { sum ->
                _state.update { it.copy(totalWeightRecycledKg = sum ?: 0f) }
            }
        }

        // Get count of verified transfers (which correspond to certifications)
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            transferDao.getCountByRecyclerAndStatus(user.id, "VERIFIED").collect { count ->
                _state.update { it.copy(certificateCount = count) }
            }
        }

        // Fetch won and open available auctions from all factory lots
        viewModelScope.launch {
            bidDao.getAllRequests().collect { requests ->
                val won = requests.filter { it.status == "AWARDED" }
                val open = requests.filter { it.status == "OPEN" }
                _state.update {
                    it.copy(
                        wonAuctions = won,
                        wonBidsCount = won.size,
                        openAuctions = open,
                        activeBidsCount = open.size
                    )
                }
            }
        }
    }

    fun initiatePickup(requestId: String, vehicleNumber: String) {
        viewModelScope.launch {
            try {
                val user = currentUser
                if (user == null) {
                    _state.update { it.copy(error = "Recycler account is not ready") }
                    return@launch
                }
                val now = System.currentTimeMillis()
                val transferId = UUID.randomUUID().toString()
                
                val request = bidDao.getRequestById(requestId) ?: return@launch
                val supervisorId = if (request.createdByUserId.isNotBlank()) request.createdByUserId else "supervisor-001"
                val factoryId = request.factoryId.ifBlank { Constants.DEFAULT_FACTORY_ID }

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
                    fromFactoryId = factoryId,
                    toRecyclerId = user.id,
                    supervisorId = supervisorId,
                    weightAtSource = request.estimatedWeightKg,
                    vehicleNumber = vehicleNumber,
                    status = "IN_TRANSIT",
                    syncStatus = "PENDING",
                    contentHash = contentHash,
                    initiatedAt = now
                )
                
                transferDao.insert(transfer)
                cloudSyncEngine.pushTransfer(transfer)

                // Update BidRequest status to CLOSED/DISPATCHED
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
                val user = currentUser
                if (user == null) {
                    _state.update { it.copy(error = "Recycler account is not ready") }
                    return@launch
                }
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
                cloudSyncEngine.pushTransfer(updatedTransfer)

                if (finalStatus == "VERIFIED") {
                    // Generate MPCB certificate automatically
                    val certId = UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
                    val payload = mapOf(
                        "certificateId" to certId,
                        "type" to "MPCB_DISPOSAL",
                        "factoryId" to transfer.fromFactoryId,
                        "factoryName" to "Ambad MIDC Pilot Unit",
                        "recyclerId" to user.id,
                        "recyclerName" to user.name,
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
                    cloudSyncEngine.pushCertificate(certificate)
                    
                    _state.update { it.copy(successMessage = "Weight verified successfully! MPCB Form 10 certificate generated.") }
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
