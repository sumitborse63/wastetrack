package com.sktech.wastetrack.ui.screens.bin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.BinDao
import com.sktech.wastetrack.data.local.db.entity.BinEntity
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class BinViewModel @Inject constructor(
    private val binDao: BinDao
) : ViewModel() {

    private val factoryId = Constants.DEFAULT_FACTORY_ID

    val bins: StateFlow<List<BinEntity>> = binDao.getByFactory(factoryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSampleBin() {
        viewModelScope.launch {
            val categories = ScrapCategory.entries.toList()
            val category = categories[Random.nextInt(categories.size)]
            val capacity = listOf(100f, 200f, 500f, 1000f)[Random.nextInt(4)]
            val fill = Random.nextFloat() * capacity

            binDao.insert(
                BinEntity(
                    id = UUID.randomUUID().toString(),
                    factoryId = factoryId,
                    scrapCategory = category.name,
                    capacityKg = capacity,
                    currentFillKg = fill,
                    fillPercentage = (fill / capacity) * 100f,
                    status = "ACTIVE"
                )
            )
        }
    }

    fun updateBinFill(binId: String) {
        viewModelScope.launch {
            val bin = binDao.getById(binId) ?: return@launch
            val newFill = (bin.currentFillKg + Random.nextFloat() * 50f).coerceAtMost(bin.capacityKg)
            val newPct = (newFill / bin.capacityKg) * 100f
            binDao.updateFillLevel(binId, newFill, newPct)
        }
    }
}
