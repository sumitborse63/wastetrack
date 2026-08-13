package com.sktech.wastetrack.ui.screens.dashboard

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.data.ml.OCRProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LedgerScanState(
    val extractedText: String = "",
    val isProcessing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LedgerScanViewModel @Inject constructor(
    private val ocrProcessor: OCRProcessor
) : ViewModel() {

    private val _state = MutableStateFlow(LedgerScanState())
    val state = _state.asStateFlow()

    fun processLedgerImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            
            val text = ocrProcessor.processImage(bitmap)
            
            if (text.startsWith("Error")) {
                _state.update { it.copy(isProcessing = false, error = text) }
            } else {
                _state.update { it.copy(isProcessing = false, extractedText = text) }
            }
        }
    }

    fun clearResult() {
        _state.update { LedgerScanState() }
    }
}
