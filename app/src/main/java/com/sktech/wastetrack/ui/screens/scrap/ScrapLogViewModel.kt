package com.sktech.wastetrack.ui.screens.scrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
import com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity
import com.sktech.wastetrack.data.sync.CloudSyncEngine
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.domain.usecase.scrap.DetectAnomalyUseCase
import com.sktech.wastetrack.util.Constants
import com.sktech.wastetrack.util.HashUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ScrapLogState(
    val selectedCategory: ScrapCategory? = null,
    val weightKg: String = "",
    val notes: String = "",
    val subCategory: String = "",
    val imageUri: String? = null,
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
    private val detectAnomalyUseCase: DetectAnomalyUseCase,
    private val authRepository: IAuthRepository,
    private val cloudSyncEngine: CloudSyncEngine
) : ViewModel() {

    private val _state = MutableStateFlow(ScrapLogState())
    val state: StateFlow<ScrapLogState> = _state.asStateFlow()

    private var factoryId: String? = null
    private var userId: String? = null

    init {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _state.update { it.copy(error = "Sign in to log scrap") }
                return@launch
            }
            factoryId = user.factoryId
            userId = user.id
            scrapEntryDao.getByFactory(user.factoryId.ifBlank { Constants.DEFAULT_FACTORY_ID }).collect { entries ->
                _state.update { it.copy(recentEntries = entries.take(10)) }
            }
        }
    }

    fun onCategorySelected(category: ScrapCategory) {
        _state.update {
            it.copy(
                selectedCategory = category,
                subCategory = "",
                error = null
            )
        }
    }

    fun onSubCategoryChanged(sub: String) {
        _state.update { it.copy(subCategory = sub) }
    }

    fun onWeightChanged(weight: String) {
        _state.update { it.copy(weightKg = weight, error = null) }
    }

    fun onNotesChanged(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    fun onImageUriChanged(uri: String?) {
        _state.update { it.copy(imageUri = uri) }
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
        val currentFactoryId = factoryId?.ifBlank { Constants.DEFAULT_FACTORY_ID } ?: Constants.DEFAULT_FACTORY_ID
        val currentUserId = userId ?: "supervisor-001"
        
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
                    factoryId = currentFactoryId,
                    timestamp = timestamp
                )

                val entity = ScrapEntryEntity(
                    id = entryId,
                    factoryId = currentFactoryId,
                    loggedByUserId = currentUserId,
                    category = current.selectedCategory.name,
                    subCategory = current.subCategory,
                    weightKg = weight,
                    estimatedVolumeL = 1000f,
                    anomalyScore = anomalyResult.score,
                    anomalyFlagged = anomalyResult.flagged,
                    imageUri = current.imageUri ?: current.selectedCategory.sampleImageUrl,
                    notes = current.notes,
                    syncStatus = "PENDING",
                    contentHash = contentHash,
                    createdAt = timestamp
                )

                // Push to Firestore & Room DB instantly
                cloudSyncEngine.pushScrapEntry(entity)

                _state.update {
                    it.copy(
                        isSubmitting = false,
                        isSuccess = true,
                        weightKg = "",
                        notes = "",
                        subCategory = "",
                        imageUri = null,
                        anomalyWarning = if (anomalyResult.flagged) "Warning: Abnormal weight recorded for ${current.selectedCategory.displayName}" else null
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSubmitting = false, error = e.message) }
            }
        }
    }

    fun resetState() {
        _state.update {
            it.copy(
                isSuccess = false,
                anomalyWarning = null,
                error = null
            )
        }
    }
}
