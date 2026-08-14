package com.sktech.wastetrack.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.BinDao
import com.sktech.wastetrack.data.local.db.dao.CertificateDao
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
import com.sktech.wastetrack.data.local.db.dao.TransferDao
import com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity
import com.sktech.wastetrack.domain.model.User
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.util.Constants
import com.sktech.wastetrack.util.DateUtils
import com.sktech.wastetrack.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val currentUser: User? = null,
    val todayScrapCount: Int = 0,
    val todayWeightKg: Float = 0f,
    val pendingTransfers: Int = 0,
    val activeBids: Int = 0,
    val binAlerts: Int = 0,
    val certificateCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val isOnline: Boolean = false,
    val recentEntries: List<ScrapEntryEntity> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val scrapEntryDao: ScrapEntryDao,
    private val transferDao: TransferDao,
    private val binDao: BinDao,
    private val certificateDao: CertificateDao,
    private val syncQueueDao: SyncQueueDao,
    private val networkMonitor: NetworkMonitor,
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val startOfDay = DateUtils.getStartOfDay()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            val factoryId = user?.factoryId?.ifBlank { Constants.DEFAULT_FACTORY_ID } ?: Constants.DEFAULT_FACTORY_ID

            _state.value = _state.value.copy(currentUser = user)

            combine(
                scrapEntryDao.getCountSince(factoryId, startOfDay),
                scrapEntryDao.getTotalWeightSince(factoryId, startOfDay),
                transferDao.getActiveTransferCount(factoryId),
                binDao.getAlertCount(factoryId),
                certificateDao.getCount(factoryId),
                syncQueueDao.getCount(),
                scrapEntryDao.getByFactory(factoryId),
                networkMonitor.isOnline
            ) { values: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                val entries = values[6] as List<ScrapEntryEntity>
                DashboardState(
                    currentUser = user,
                    todayScrapCount = values[0] as Int,
                    todayWeightKg = (values[1] as? Float) ?: 0f,
                    pendingTransfers = values[2] as Int,
                    binAlerts = values[3] as Int,
                    certificateCount = values[4] as Int,
                    pendingSyncCount = values[5] as Int,
                    isOnline = values[7] as Boolean,
                    recentEntries = entries.take(5)
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }
}
