package com.sktech.wastetrack.ui.screens.scrap

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.ml.ClassificationResult
import com.sktech.wastetrack.domain.usecase.scrap.ClassifyScrapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScrapClassifyState(
    val isAnalyzing: Boolean = false,
    val result: ClassificationResult? = null,
    val capturedImage: Bitmap? = null,
    val error: String? = null
)

@HiltViewModel
class ScrapClassifyViewModel @Inject constructor(
    private val classifyScrapUseCase: ClassifyScrapUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ScrapClassifyState())
    val state: StateFlow<ScrapClassifyState> = _state.asStateFlow()

    fun analyzeImage(bitmap: Bitmap, rotationDegrees: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true, error = null, capturedImage = bitmap, result = null) }
            try {
                val classification = classifyScrapUseCase(bitmap, rotationDegrees)
                _state.update { 
                    it.copy(isAnalyzing = false, result = classification) 
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(isAnalyzing = false, error = "Failed to classify image: ${e.message}") 
                }
            }
        }
    }

    fun reset() {
        _state.value = ScrapClassifyState()
    }
}
