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

    private val factoryId = Constants.DEFAULT_FACTORY_ID
    private val userId = "pilot-user-001"

    private val _state = MutableStateFlow(TransferState())
    val state: StateFlow<TransferState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            val isRecycler = user?.role == com.sktech.wastetrack.domain.model.UserRole.RECYCLER
            
            if (isRecycler && user != null) {
                transferDao.getByRecycler(user.id).collect { transfers ->
                    _state.update { it.copy(transfers = transfers) }
                }
            } else {
                transferDao.getByFactory(factoryId).collect { transfers ->
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
            _state.update { it.copy(isCreating = true) }
            try {
                val transferId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                val contentHash = HashUtils.hashTransfer(
                    id = transferId,
                    scrapEntryId = scrapEntryId,
                    weightAtSource = weightKg,
                    supervisorId = userId,
                    timestamp = timestamp
                )

                val transfer = TransferEntity(
                    id = transferId,
                    scrapEntryId = scrapEntryId,
                    fromFactoryId = factoryId,
                    supervisorId = userId,
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
                    "factoryId" to factoryId,
                    "weightKg" to weightKg,
                    "supervisorId" to userId,
                    "timestamp" to timestamp.toString(),
                    "contentHash" to contentHash
                )
                val payload = Gson().toJson(qrData)

                val handshake = QRHandshakeEntity(
                    id = UUID.randomUUID().toString(),
                    transferId = transferId,
                    qrPayload = payload,
                    supervisorSignature = userId,
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

    fun verifyQRHandshake(payload: String): Boolean {
        return try {
            val data = Gson().fromJson(payload, Map::class.java)
            data.containsKey("transferId") && data.containsKey("contentHash")
        } catch (e: Exception) {
            false
        }
    }

    fun clearQR() {
        _state.update { it.copy(qrPayload = null) }
    }
}
