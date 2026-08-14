package com.sktech.wastetrack.ui.screens.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.TransferDao
import com.sktech.wastetrack.data.local.db.dao.QRHandshakeDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
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
    val error: String? = null
)

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val transferDao: TransferDao,
    private val scrapEntryDao: ScrapEntryDao,
    private val qrHandshakeDao: QRHandshakeDao,
    private val syncQueueDao: SyncQueueDao,
    private val authRepository: com.sktech.wastetrack.domain.repository.IAuthRepository
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
            val authenticatedFactoryId = user.factoryId
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

                // Enqueue sync
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        entityType = "TRANSFER",
                        entityId = transferId,
                        action = "CREATE",
                        payload = Gson().toJson(transfer)
                    )
                )

                _state.update {
                    it.copy(
                        transfers = listOf(transfer) + it.transfers.filter { t -> t.id != transfer.id },
                        isCreating = false,
                        qrPayload = payload,
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
                transfer.status == "QR_GENERATED" &&
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
                transferDao.updateStatus(transferId, "QR_SCANNED")
                _state.update { current ->
                    current.copy(
                        transfers = current.transfers.map { if (it.id == transferId) it.copy(status = "QR_SCANNED") else it }
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
}
