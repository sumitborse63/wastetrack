package com.sktech.wastetrack.ui.screens.scrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
import com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity
import com.sktech.wastetrack.data.local.db.entity.SyncQueueEntity
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.util.Constants
import com.sktech.wastetrack.util.HashUtils
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.sktech.wastetrack.domain.usecase.scrap.DetectAnomalyUseCase
import java.util.UUID
import javax.inject.Inject

data class ScrapLogState(
    val selectedCategory: ScrapCategory? = null,
    val weightKg: String = "",
    val notes: String = "",
    val subCategory: String = "",
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val anomalyWarning: String? = null,
    val error: String? = null,
    val recentEntries: List<ScrapEntryEntity> = emptyList()
)

@HiltViewModel
class ScrapLogViewModel @Inject constructor(
    private val scrapEntryDao: ScrapEntryDao,
    private val syncQueueDao: SyncQueueDao,
    private val detectAnomalyUseCase: DetectAnomalyUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ScrapLogState())
    val state: StateFlow<ScrapLogState> = _state.asStateFlow()

    private val factoryId = Constants.DEFAULT_FACTORY_ID
    private val userId = "pilot-user-001" // Will come from auth

    init {
        viewModelScope.launch {
            scrapEntryDao.getByFactory(factoryId).collect { entries ->
                _state.update { it.copy(recentEntries = entries.take(10)) }
            }
        }
    }

    fun onCategorySelected(category: ScrapCategory) {
        _state.update { it.copy(selectedCategory = category, error = null) }
    }

    fun onWeightChanged(weight: String) {
        _state.update { it.copy(weightKg = weight, error = null) }
    }

    fun onNotesChanged(notes: String) {
        _state.update { it.copy(notes = notes, anomalyWarning = null) }
    }

    fun onSubCategoryChanged(sub: String) {
        _state.update { it.copy(subCategory = sub, anomalyWarning = null) }
    }

    fun submitEntry() {
        val current = _state.value
        if (current.selectedCategory == null) {
            _state.update { it.copy(error = "Please select a scrap category") }
            return
        }
        val weight = current.weightKg.toFloatOrNull()
        if (weight == null || weight <= 0) {
            _state.update { it.copy(error = "Please enter a valid weight") }
            return
        }
        
        // If an anomaly warning was shown and they click submit again, we bypass it
        // Or we can just flag it in the DB. Let's just flag it in the DB for now.
        val anomalyResult = detectAnomalyUseCase(current.selectedCategory, weight)

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            try {
                val entryId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                val contentHash = HashUtils.hashScrapEntry(
                    id = entryId,
                    category = current.selectedCategory.name,
                    weightKg = weight,
                    factoryId = factoryId,
                    timestamp = timestamp
                )

                val entity = ScrapEntryEntity(
                    id = entryId,
                    factoryId = factoryId,
                    loggedByUserId = userId,
                    category = current.selectedCategory.name,
                    subCategory = current.subCategory,
                    weightKg = weight,
                    estimatedVolumeL = 1000f, // Default 1m3 for MVP
                    anomalyScore = anomalyResult.score,
                    anomalyFlagged = anomalyResult.flagged,
                    notes = current.notes,
                    syncStatus = "PENDING",
                    contentHash = contentHash,
                    createdAt = timestamp
                )

                scrapEntryDao.insert(entity)

                // Enqueue for sync
                syncQueueDao.enqueue(
                    SyncQueueEntity(
                        entityType = "SCRAP_ENTRY",
                        entityId = entryId,
                        action = "CREATE",
                        payload = Gson().toJson(entity)
                    )
                )

                _state.update {
                    ScrapLogState(isSuccess = true, recentEntries = it.recentEntries)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSubmitting = false, error = e.message ?: "Failed to log entry")
                }
            }
        }
    }

    fun resetState() {
        _state.update { ScrapLogState(recentEntries = it.recentEntries) }
    }
}
