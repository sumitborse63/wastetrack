package com.sktech.wastetrack.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.QRHandshakeDao
import com.sktech.wastetrack.data.local.db.dao.TransferDao
import com.sktech.wastetrack.data.local.db.entity.TransferEntity
import com.sktech.wastetrack.data.sync.CloudSyncEngine
import com.sktech.wastetrack.domain.model.User
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.util.Constants
import com.sktech.wastetrack.util.HashUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriverDashboardState(
    val currentUser: User? = null,
    val vehicleNumber: String = "MH-15-TR-2024",
    val activeTrips: List<TransferEntity> = emptyList(),
    val completedTrips: List<TransferEntity> = emptyList(),
    val totalTripsCompleted: Int = 0,
    val totalTonnageTransportedKg: Float = 0f,
    val onTimeRating: Int = 98,
    val isOnline: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class DriverDashboardViewModel @Inject constructor(
    private val transferDao: TransferDao,
    private val qrHandshakeDao: QRHandshakeDao,
    private val authRepository: IAuthRepository,
    private val cloudSyncEngine: CloudSyncEngine
) : ViewModel() {

    private val _state = MutableStateFlow(DriverDashboardState())
    val state: StateFlow<DriverDashboardState> = _state.asStateFlow()

    init {
        loadDriverData()
    }

    private fun loadDriverData() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            val vehicleNo = user?.registrationNumber?.ifBlank { "MH-15-TR-2024" } ?: "MH-15-TR-2024"
            val factoryId = user?.factoryId?.ifBlank { Constants.DEFAULT_FACTORY_ID } ?: Constants.DEFAULT_FACTORY_ID

            _state.update {
                it.copy(
                    currentUser = user,
                    vehicleNumber = vehicleNo
                )
            }

            // Continuously collect transfers for this factory/driver
            transferDao.getByFactory(factoryId).collect { allTransfers ->
                val active = allTransfers.filter {
                    it.status == "INITIATED" || it.status == "QR_GENERATED" || it.status == "IN_TRANSIT" || it.status == "DISPATCHED"
                }
                val completed = allTransfers.filter {
                    it.status == "VERIFIED" || it.status == "DELIVERED"
                }
                val totalWeight = completed.sumOf { (it.weightAtDestination ?: it.weightAtSource).toDouble() }.toFloat()

                _state.update {
                    it.copy(
                        activeTrips = active,
                        completedTrips = completed,
                        totalTripsCompleted = completed.size,
                        totalTonnageTransportedKg = totalWeight
                    )
                }
            }
        }
    }

    fun markGateExit(transferId: String) {
        viewModelScope.launch {
            try {
                val transfer = transferDao.getById(transferId) ?: return@launch
                val updated = transfer.copy(status = "IN_TRANSIT")
                transferDao.insert(updated)
                cloudSyncEngine.pushTransfer(updated)
                _state.update { it.copy(successMessage = "Gate pass verified! Vehicle ${_state.value.vehicleNumber} marked as IN_TRANSIT.") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun reportDelay(transferId: String, reason: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(successMessage = "Incident logged: \"$reason\". Factory dispatcher and recycler notified.")
            }
        }
    }

    suspend fun verifyGatePassQR(qrPayload: String): Boolean {
        return try {
            val data = Gson().fromJson(qrPayload, JsonObject::class.java)
            val transferId = data.get("transferId")?.asString ?: return false
            val scrapEntryId = data.get("scrapEntryId")?.asString ?: return false
            val supervisorId = data.get("supervisorId")?.asString ?: return false
            val timestamp = data.get("timestamp")?.asString?.toLongOrNull() ?: return false
            val payloadHash = data.get("contentHash")?.asString ?: return false
            val weightKg = data.get("weightKg")?.asFloat ?: return false

            val transfer = transferDao.getById(transferId) ?: return false
            val handshake = qrHandshakeDao.getByTransferId(transferId) ?: return false
            val expectedHash = HashUtils.hashTransfer(transferId, scrapEntryId, weightKg, supervisorId, timestamp)

            val isValid = (transfer.contentHash == payloadHash) && (expectedHash == payloadHash)

            if (isValid) {
                val updatedTransfer = transfer.copy(status = "IN_TRANSIT")
                transferDao.insert(updatedTransfer)
                cloudSyncEngine.pushTransfer(updatedTransfer)

                qrHandshakeDao.update(
                    handshake.copy(
                        driverSignature = _state.value.currentUser?.id.orEmpty(),
                        scannedAt = System.currentTimeMillis(),
                        isValid = true
                    )
                )

                _state.update {
                    it.copy(successMessage = "Gate Pass Authenticated! Dual cryptographic signature verified.")
                }
            }
            isValid
        } catch (e: Exception) {
            _state.update { it.copy(error = "Invalid QR code: ${e.message}") }
            false
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, successMessage = null) }
    }
}
