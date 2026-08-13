package com.sktech.wastetrack.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.BinDao
import com.sktech.wastetrack.data.local.db.dao.CertificateDao
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
import com.sktech.wastetrack.data.local.db.dao.TransferDao
import com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity
import com.sktech.wastetrack.util.Constants
import com.sktech.wastetrack.util.DateUtils
import com.sktech.wastetrack.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardState(
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
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val factoryId = Constants.DEFAULT_FACTORY_ID
    private val startOfDay = DateUtils.getStartOfDay()

    val state: StateFlow<DashboardState> = combine(
        scrapEntryDao.getCountSince(factoryId, startOfDay),
        scrapEntryDao.getTotalWeightSince(factoryId, startOfDay),
        transferDao.getCountByStatus(factoryId, "INITIATED"),
        binDao.getAlertCount(factoryId),
        certificateDao.getCount(factoryId),
        syncQueueDao.getCount(),
        scrapEntryDao.getByFactory(factoryId)
    ) { values ->
        DashboardState(
            todayScrapCount = values[0] as Int,
            todayWeightKg = (values[1] as? Float) ?: 0f,
            pendingTransfers = values[2] as Int,
            binAlerts = values[3] as Int,
            certificateCount = values[4] as Int,
            pendingSyncCount = values[5] as Int,
            isOnline = networkMonitor.isOnline.value,
            recentEntries = (values[6] as List<ScrapEntryEntity>).take(5)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardState()
    )
}
