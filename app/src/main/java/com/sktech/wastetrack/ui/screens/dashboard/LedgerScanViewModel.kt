package com.sktech.wastetrack.ui.screens.dashboard

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sktech.wastetrack.data.local.db.dao.ScrapEntryDao
import com.sktech.wastetrack.data.local.db.dao.SyncQueueDao
import com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity
import com.sktech.wastetrack.data.local.db.entity.SyncQueueEntity
import com.sktech.wastetrack.data.ml.OCRProcessor
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.util.Constants
import com.sktech.wastetrack.util.HashUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject

data class ParsedLedgerEntry(
    val id: String = UUID.randomUUID().toString(),
    val category: ScrapCategory,
    val subCategory: String,
    val weightKg: Float,
    val rawLine: String
)

data class LedgerScanState(
    val extractedText: String = "",
    val parsedEntries: List<ParsedLedgerEntry> = emptyList(),
    val isProcessing: Boolean = false,
    val isImporting: Boolean = false,
    val importSuccessMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class LedgerScanViewModel @Inject constructor(
    private val ocrProcessor: OCRProcessor,
    private val scrapEntryDao: ScrapEntryDao,
    private val syncQueueDao: SyncQueueDao,
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LedgerScanState())
    val state = _state.asStateFlow()

    fun processLedgerImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null, importSuccessMessage = null) }
            
            val text = ocrProcessor.processImage(bitmap)
            
            if (text.startsWith("Error")) {
                _state.update { it.copy(isProcessing = false, error = text) }
            } else {
                val parsed = parseLedgerText(text)
                _state.update { 
                    it.copy(
                        isProcessing = false, 
                        extractedText = text,
                        parsedEntries = parsed
                    ) 
                }
            }
        }
    }

    private fun parseLedgerText(text: String): List<ParsedLedgerEntry> {
        val lines = text.split("\n", "\r\n")
        val results = mutableListOf<ParsedLedgerEntry>()

        val weightRegex = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(kg|kgs|kilo|kilogram|ton|tons|t|mt)?", Pattern.CASE_INSENSITIVE)

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length < 3) continue

            val lower = trimmed.lowercase()

            // 1. Detect Category
            val category = when {
                lower.contains("metal") || lower.contains("steel") || lower.contains("iron") || lower.contains("aluminum") || lower.contains("copper") || lower.contains("brass") || lower.contains("hms") -> ScrapCategory.METAL
                lower.contains("plastic") || lower.contains("hdpe") || lower.contains("ldpe") || lower.contains("pet") || lower.contains("pp") || lower.contains("pvc") || lower.contains("polymer") -> ScrapCategory.PLASTIC
                lower.contains("paper") || lower.contains("cardboard") || lower.contains("box") || lower.contains("carton") || lower.contains("occ") -> ScrapCategory.PAPER
                lower.contains("ewaste") || lower.contains("e-waste") || lower.contains("battery") || lower.contains("pcb") || lower.contains("electronic") || lower.contains("wire") -> ScrapCategory.EWASTE
                lower.contains("glass") || lower.contains("bottle") || lower.contains("cullet") -> ScrapCategory.GLASS
                lower.contains("rubber") || lower.contains("tire") || lower.contains("tyre") -> ScrapCategory.RUBBER
                lower.contains("wood") || lower.contains("pallet") || lower.contains("timber") -> ScrapCategory.WOOD
                lower.contains("chemical") || lower.contains("sludge") || lower.contains("oil") || lower.contains("solvent") -> ScrapCategory.CHEMICAL
                else -> ScrapCategory.OTHER
            }

            // 2. Extract Weight
            val matcher = weightRegex.matcher(trimmed)
            var weightKg = 0f
            while (matcher.find()) {
                val numStr = matcher.group(1)
                val unit = matcher.group(3)?.lowercase()
                val value = numStr?.toFloatOrNull() ?: 0f
                if (value > 0f) {
                    weightKg = if (unit == "ton" || unit == "tons" || unit == "t" || unit == "mt") {
                        value * 1000f
                    } else {
                        value
                    }
                    break
                }
            }

            if (weightKg > 0f) {
                val subCategory = when (category) {
                    ScrapCategory.METAL -> "Industrial Metal Turnings"
                    ScrapCategory.PLASTIC -> "Polymer Scrap / Drums"
                    ScrapCategory.PAPER -> "OCC Corrugated Boxes"
                    ScrapCategory.EWASTE -> "Electronic Components"
                    ScrapCategory.GLASS -> "Industrial Glass Cullet"
                    ScrapCategory.RUBBER -> "Tire / Conveyor Shreds"
                    ScrapCategory.WOOD -> "Pallet / Packing Timber"
                    ScrapCategory.CHEMICAL -> "Chemical Waste"
                    ScrapCategory.OTHER -> "Mixed Solid Scrap"
                }

                results.add(
                    ParsedLedgerEntry(
                        category = category,
                        subCategory = subCategory,
                        weightKg = weightKg,
                        rawLine = trimmed
                    )
                )
            }
        }

        return results
    }

    fun importParsedEntries() {
        val entries = _state.value.parsedEntries
        if (entries.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isImporting = true) }
            try {
                val user = authRepository.getCurrentUser()
                val factoryId = user?.factoryId?.ifBlank { Constants.DEFAULT_FACTORY_ID } ?: Constants.DEFAULT_FACTORY_ID
                val userId = user?.id ?: "supervisor-legacy"
                val now = System.currentTimeMillis()

                val entities = entries.map { entry ->
                    val id = UUID.randomUUID().toString()
                    val hash = HashUtils.hashScrapEntry(
                        id = id,
                        category = entry.category.name,
                        weightKg = entry.weightKg,
                        factoryId = factoryId,
                        timestamp = now
                    )
                    ScrapEntryEntity(
                        id = id,
                        factoryId = factoryId,
                        loggedByUserId = userId,
                        category = entry.category.name,
                        subCategory = entry.subCategory,
                        weightKg = entry.weightKg,
                        estimatedVolumeL = 1000f,
                        anomalyScore = 0.05f,
                        anomalyFlagged = false,
                        notes = "Imported from Legacy Paper Ledger: ${entry.rawLine}",
                        syncStatus = "PENDING",
                        contentHash = hash,
                        createdAt = now
                    )
                }

                scrapEntryDao.insertAll(entities)

                for (entity in entities) {
                    syncQueueDao.enqueue(
                        SyncQueueEntity(
                            entityType = "SCRAP_ENTRY",
                            entityId = entity.id,
                            action = "CREATE",
                            payload = Gson().toJson(entity)
                        )
                    )
                }

                _state.update {
                    it.copy(
                        isImporting = false,
                        importSuccessMessage = "Successfully digitized & imported ${entities.size} records into the Scrap Log!",
                        parsedEntries = emptyList()
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isImporting = false, error = e.message ?: "Failed to import records") }
            }
        }
    }

    fun clearResult() {
        _state.update { LedgerScanState() }
    }
}
