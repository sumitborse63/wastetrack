package com.sktech.wastetrack.ui.screens.bin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.BinDao
import com.sktech.wastetrack.data.local.db.entity.BinEntity
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.domain.model.Bin
import com.sktech.wastetrack.domain.model.BinStatus
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.domain.usecase.PredictOverflowUseCase
import com.sktech.wastetrack.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BinViewModel @Inject constructor(
    private val binDao: BinDao,
    private val predictOverflow: PredictOverflowUseCase,
    private val authRepository: IAuthRepository,
    private val cloudSyncEngine: com.sktech.wastetrack.data.sync.CloudSyncEngine
) : ViewModel() {

    private val _bins = MutableStateFlow<List<BinEntity>>(emptyList())
    val bins: StateFlow<List<BinEntity>> = _bins.asStateFlow()

    private var activeFactoryId: String = Constants.DEFAULT_FACTORY_ID

    init {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            activeFactoryId = user?.factoryId?.ifBlank { Constants.DEFAULT_FACTORY_ID } ?: Constants.DEFAULT_FACTORY_ID
            binDao.getByFactory(activeFactoryId).collect { list ->
                _bins.value = list
            }
        }
    }

    fun addBin(category: ScrapCategory, capacityKg: Float) {
        if (capacityKg <= 0f) return
        viewModelScope.launch {
            binDao.insert(
                BinEntity(
                    id = UUID.randomUUID().toString(),
                    factoryId = activeFactoryId,
                    scrapCategory = category.name,
                    capacityKg = capacityKg,
                    status = "ACTIVE"
                )
            )
        }
    }

    fun updateBinFill(binId: String, measuredFillKg: Float) {
        viewModelScope.launch {
            val entity = binDao.getById(binId) ?: return@launch
            val newFill = measuredFillKg.coerceIn(0f, entity.capacityKg)
            val newPct = (newFill / entity.capacityKg) * 100f
            
            // Domain model mapping to calculate prediction
            val domainBin = Bin(
                id = entity.id,
                factoryId = entity.factoryId,
                scrapCategory = ScrapCategory.valueOf(entity.scrapCategory),
                capacityKg = entity.capacityKg,
                currentFillKg = newFill,
                fillPercentage = newPct,
                predictedFullTimestamp = entity.predictedFullTimestamp,
                status = BinStatus.valueOf(entity.status)
            )
            
            val prediction = predictOverflow(domainBin)
            
            binDao.updateFillLevel(binId, newFill, newPct, prediction)
        }
    }
}
