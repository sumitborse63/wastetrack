package com.sktech.wastetrack.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsState(
    val isLoading: Boolean = false,
    val monthlyScrapKg: List<Float> = emptyList(),
    val categoryBreakdown: Map<String, Float> = emptyMap(),
    val efficiencyScore: Int = 0,
    val error: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            // Simulate network delay
            kotlinx.coroutines.delay(1000)
            
            // Mock data for MVP
            _state.update {
                it.copy(
                    isLoading = false,
                    monthlyScrapKg = listOf(1200f, 1500f, 1100f, 1800f, 2100f, 1950f), // Last 6 months
                    categoryBreakdown = mapOf(
                        "Metal" to 45f,
                        "Plastic" to 30f,
                        "Rubber" to 15f,
                        "E-Waste" to 10f
                    ),
                    efficiencyScore = 92
                )
            }
        }
    }
}
