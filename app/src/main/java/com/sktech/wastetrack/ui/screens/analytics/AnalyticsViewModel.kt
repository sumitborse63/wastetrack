package com.sktech.wastetrack.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.TransferDao
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AnalyticsState(
    val isLoading: Boolean = true,
    val monthlyScrapKg: List<Float> = emptyList(),
    val categoryBreakdown: Map<String, Float> = emptyMap(),
    val efficiencyScore: Int = 100,
    val eprCompliancePercentage: Float = 0f,
    val error: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val scrapEntryDao: ScrapEntryDao,
    private val transferDao: TransferDao,
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                val factoryId = user?.factoryId ?: Constants.DEFAULT_FACTORY_ID

                combine(
                    scrapEntryDao.getByFactory(factoryId),
                    transferDao.getByFactory(factoryId)
                ) { entries, transfers ->
                    Pair(entries, transfers)
                }.collect { (entries, transfers) ->
                    if (entries.isEmpty()) {
                        _state.update { 
                            it.copy(
                                isLoading = false, 
                                monthlyScrapKg = List(6) { 0f },
                                categoryBreakdown = emptyMap(),
                                efficiencyScore = 100,
                                eprCompliancePercentage = 0f
                            ) 
                        }
                        return@collect
                    }

                    // 1. Calculate Category Breakdown
                    val totalWeight = entries.sumOf { it.weightKg.toDouble() }.toFloat()
                    val breakdown = mutableMapOf<String, Float>()
                    if (totalWeight > 0) {
                        val grouped = entries.groupBy { it.category }
                        grouped.forEach { (cat, list) ->
                            val catWeight = list.sumOf { it.weightKg.toDouble() }.toFloat()
                            val percentage = (catWeight / totalWeight) * 100f
                            val catName = try { 
                                com.sktech.wastetrack.domain.model.ScrapCategory.valueOf(cat).displayName 
                            } catch (e: Exception) { cat }
                            breakdown[catName] = percentage
                        }
                    }

                    // 2. Calculate Efficiency Score
                    val anomalies = entries.count { it.anomalyFlagged }
                    val score = (100 - (anomalies * 5)).coerceIn(0, 100)

                    // 3. EPR Compliance (Verified Recycled / Total Generated)
                    val verifiedTransfers = transfers.filter { it.status == "VERIFIED" }
                    val totalRecycled = verifiedTransfers.sumOf { (it.weightAtDestination ?: 0f).toDouble() }.toFloat()
                    val eprPercentage = if (totalWeight > 0) ((totalRecycled / totalWeight) * 100f).coerceIn(0f, 100f) else 0f

                    // 4. Calculate 6-Month Trend
                    val calendar = Calendar.getInstance()
                    val currentMonth = calendar.get(Calendar.MONTH)
                    val currentYear = calendar.get(Calendar.YEAR)
                    
                    val trend = FloatArray(6) { 0f }
                    
                    entries.forEach { entry ->
                        calendar.timeInMillis = entry.createdAt
                        val entryMonth = calendar.get(Calendar.MONTH)
                        val entryYear = calendar.get(Calendar.YEAR)
                        val monthsAgo = (currentYear - entryYear) * 12 + (currentMonth - entryMonth)
                        
                        if (monthsAgo in 0..5) {
                            val index = 5 - monthsAgo
                            trend[index] += entry.weightKg
                        }
                    }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            monthlyScrapKg = trend.toList(),
                            categoryBreakdown = breakdown.entries.sortedByDescending { e -> e.value }.associate { e -> e.key to e.value },
                            efficiencyScore = score,
                            eprCompliancePercentage = eprPercentage
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
