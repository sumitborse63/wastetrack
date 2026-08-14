package com.sktech.wastetrack.ui.screens.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.CertificateDao
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.TransferDao
import com.sktech.wastetrack.data.local.db.dao.QRHandshakeDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
import com.sktech.wastetrack.data.local.db.entity.CertificateEntity
import com.sktech.wastetrack.data.local.db.entity.TransferEntity
import com.sktech.wastetrack.data.local.db.entity.QRHandshakeEntity
import com.sktech.wastetrack.data.local.db.entity.SyncQueueEntity
import com.sktech.wastetrack.util.Constants
import com.sktech.wastetrack.util.HashUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TransferState(
    val transfers: List<TransferEntity> = emptyList(),
    val selectedScrapEntryId: String? = null,
    val vehicleNumber: String = "",
    val isCreating: Boolean = false,
    val qrPayload: String? = null,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val transferDao: TransferDao,
    private val scrapEntryDao: ScrapEntryDao,
    private val qrHandshakeDao: QRHandshakeDao,
    private val certificateDao: CertificateDao,
    private val syncQueueDao: SyncQueueDao,
    private val authRepository: com.sktech.wastetrack.domain.repository.IAuthRepository,
    private val cloudSyncEngine: com.sktech.wastetrack.data.sync.CloudSyncEngine
) : ViewModel() {

    private var factoryId: String? = null
    private var userId: String? = null

    private val _state = MutableStateFlow(TransferState())
    val state: StateFlow<TransferState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _state.update { it.copy(error = "Sign in to view transfers") }
                return@launch
            }
            val authenticatedFactoryId = user.factoryId.ifBlank { Constants.DEFAULT_FACTORY_ID }
            factoryId = authenticatedFactoryId
            userId = user.id
            val isRecycler = user.role == com.sktech.wastetrack.domain.model.UserRole.RECYCLER
            
            if (isRecycler) {
                transferDao.getByRecycler(user.id).collect { transfers ->
                    _state.update { it.copy(transfers = transfers) }
                }
            } else {
                transferDao.getByFactory(authenticatedFactoryId).collect { transfers ->
                    _state.update { it.copy(transfers = transfers) }
                }
            }
        }
    }

    fun onVehicleNumberChanged(number: String) {
        _state.update { it.copy(vehicleNumber = number) }
    }

    fun initiateTransfer(scrapEntryId: String, weightKg: Float) {
        viewModelScope.launch {
            val currentFactoryId = factoryId ?: Constants.DEFAULT_FACTORY_ID
            val currentUserId = userId ?: "supervisor-001"
            if (_state.value.vehicleNumber.trim().isBlank()) {
                _state.update { it.copy(error = "Enter a vehicle number") }
                return@launch
            }
            _state.update { it.copy(isCreating = true) }
            try {
                val transferId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                val contentHash = HashUtils.hashTransfer(
                    id = transferId,
                    scrapEntryId = scrapEntryId,
                    weightAtSource = weightKg,
                    supervisorId = currentUserId,
                    timestamp = timestamp
                )

                val transfer = TransferEntity(
                    id = transferId,
                    scrapEntryId = scrapEntryId,
                    fromFactoryId = currentFactoryId,
                    supervisorId = currentUserId,
                    weightAtSource = weightKg,
                    vehicleNumber = _state.value.vehicleNumber,
                    status = "QR_GENERATED",
                    syncStatus = "PENDING",
                    contentHash = contentHash,
                    initiatedAt = timestamp
                )
                transferDao.insert(transfer)

                // Generate QR payload
                val qrData = mapOf(
                    "transferId" to transferId,
                    "scrapEntryId" to scrapEntryId,
                    "factoryId" to currentFactoryId,
                    "weightKg" to weightKg,
                    "supervisorId" to currentUserId,
                    "timestamp" to timestamp.toString(),
                    "contentHash" to contentHash
                )
                val payload = Gson().toJson(qrData)

                val handshake = QRHandshakeEntity(
                    id = UUID.randomUUID().toString(),
                    transferId = transferId,
                    qrPayload = payload,
                    supervisorSignature = currentUserId,
                    generatedAt = timestamp
                )
                qrHandshakeDao.insert(handshake)

                // Push to Firestore & Room DB instantly
                cloudSyncEngine.pushTransfer(transfer)

                _state.update {
                    it.copy(
                        transfers = listOf(transfer) + it.transfers.filter { t -> t.id != transfer.id },
                        isCreating = false,
                        qrPayload = payload,
                        successMessage = "Gate pass generated for vehicle ${_state.value.vehicleNumber}",
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isCreating = false, error = e.message)
                }
            }
        }
    }

    /**
     * Completes destination weighbridge check and automatically creates MPCB Form 10 ESG certificate
     */
    fun verifyDestinationArrival(transferId: String, receivedWeightKg: Float) {
        viewModelScope.launch {
            try {
                val transfer = transferDao.getById(transferId) ?: return@launch
                val sourceWeight = transfer.weightAtSource
                val discrepancy = Math.abs(receivedWeightKg - sourceWeight)
                val isDisputed = discrepancy > (sourceWeight * 0.10f)
                val finalStatus = if (isDisputed) "DISPUTED" else "VERIFIED"
                val now = System.currentTimeMillis()

                val updatedTransfer = transfer.copy(
                    weightAtDestination = receivedWeightKg,
                    weightDiscrepancy = discrepancy,
                    status = finalStatus,
                    completedAt = now
                )
                cloudSyncEngine.pushTransfer(updatedTransfer)

                if (finalStatus == "VERIFIED") {
                    val certId = UUID.randomUUID().toString()
                    val payload = mapOf(
                        "certificateId" to certId,
                        "type" to "MPCB_DISPOSAL",
                        "factoryId" to transfer.fromFactoryId,
                        "factoryName" to "Ambad MIDC Industrial Unit",
                        "transferId" to transfer.id,
                        "weightDisposedKg" to receivedWeightKg,
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
                    cloudSyncEngine.pushCertificate(certificate)

                    _state.update {
                        it.copy(
                            transfers = it.transfers.map { t -> if (t.id == transferId) updatedTransfer else t },
                            successMessage = "Weighbridge verified! MPCB Form 10 Certificate #CERT-${certId.take(6).uppercase()} issued."
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            transfers = it.transfers.map { t -> if (t.id == transferId) updatedTransfer else t },
                            error = "Weight discrepancy (${sourceWeight}kg vs ${receivedWeightKg}kg) exceeds 10% tolerance! Marked as DISPUTED."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    suspend fun verifyQRHandshake(payload: String): Boolean {
        return try {
            val data = Gson().fromJson(payload, JsonObject::class.java)
            val transferId = data.get("transferId")?.asString ?: return false
            val scrapEntryId = data.get("scrapEntryId")?.asString ?: return false
            val supervisorId = data.get("supervisorId")?.asString ?: return false
            val timestamp = data.get("timestamp")?.asString?.toLongOrNull() ?: return false
            val payloadHash = data.get("contentHash")?.asString ?: return false
            val weightKg = data.get("weightKg")?.asFloat ?: return false
            val transfer = transferDao.getById(transferId) ?: return false
            val handshake = qrHandshakeDao.getByTransferId(transferId) ?: return false
            val expectedHash = HashUtils.hashTransfer(transferId, scrapEntryId, weightKg, supervisorId, timestamp)
            val isExpired = System.currentTimeMillis() > handshake.generatedAt + Constants.QR_EXPIRY_MINUTES * 60_000L
            val isValid = !isExpired &&
                (transfer.status == "QR_GENERATED" || transfer.status == "INITIATED") &&
                transfer.scrapEntryId == scrapEntryId &&
                transfer.supervisorId == supervisorId &&
                transfer.contentHash == payloadHash &&
                expectedHash == payloadHash &&
                handshake.qrPayload == payload
            if (isValid) {
                qrHandshakeDao.update(
                    handshake.copy(
                        driverSignature = userId.orEmpty(),
                        scannedAt = System.currentTimeMillis(),
                        isValid = true
                    )
                )
                transferDao.updateStatus(transferId, "IN_TRANSIT")
                _state.update { current ->
                    current.copy(
                        transfers = current.transfers.map { if (it.id == transferId) it.copy(status = "IN_TRANSIT") else it },
                        successMessage = "Gate pass validated! Truck dispatch recorded as IN_TRANSIT."
                    )
                }
            }
            isValid
        } catch (e: Exception) {
            false
        }
    }

    fun clearQR() {
        _state.update { it.copy(qrPayload = null) }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, successMessage = null) }
    }
}
