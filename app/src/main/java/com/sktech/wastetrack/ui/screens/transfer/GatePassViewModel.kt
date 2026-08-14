package com.sktech.wastetrack.ui.screens.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.QRHandshakeDao
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.TransferDao
import com.sktech.wastetrack.data.local.db.entity.TransferEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GatePassUiState(
    val transfer: TransferEntity? = null,
    val qrPayload: String? = null,
    val scrapCategoryName: String = "Industrial Scrap",
    val isLoading: Boolean = true
)

@HiltViewModel
class GatePassViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transferDao: TransferDao,
    private val qrHandshakeDao: QRHandshakeDao,
    private val scrapEntryDao: ScrapEntryDao
) : ViewModel() {

    private val transferId: String = savedStateHandle.get<String>("transferId").orEmpty()

    private val _state = MutableStateFlow(GatePassUiState())
    val state: StateFlow<GatePassUiState> = _state.asStateFlow()

    init {
        loadGatePassData()
    }

    private fun loadGatePassData() {
        viewModelScope.launch {
            if (transferId.isBlank()) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val transfer = transferDao.getById(transferId)
            val handshake = qrHandshakeDao.getByTransferId(transferId)
            val scrap = transfer?.let { scrapEntryDao.getById(it.scrapEntryId) }

            val catName = scrap?.let {
                "${it.category} ${if (it.subCategory.isNotBlank()) "(${it.subCategory})" else ""}"
            } ?: "Industrial Scrap"

            val payload = handshake?.qrPayload ?: transfer?.contentHash ?: "GATEPASS-$transferId"

            _state.update {
                it.copy(
                    transfer = transfer,
                    qrPayload = payload,
                    scrapCategoryName = catName,
                    isLoading = false
                )
            }
        }
    }
}
