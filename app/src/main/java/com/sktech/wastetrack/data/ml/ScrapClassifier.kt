package com.sktech.wastetrack.data.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.sktech.wastetrack.domain.model.ScrapCategory
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class ClassificationResult(
    val category: ScrapCategory,
    val confidence: Float,
    val rawLabels: List<String>
)

@Singleton
class ScrapClassifier @Inject constructor() {
    // We use the default Image Labeler which provides 400+ generic labels
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.6f)
            .build()
    )

    suspend fun classifyImage(bitmap: Bitmap, rotationDegrees: Int): ClassificationResult {
        try {
            val image = InputImage.fromBitmap(bitmap, rotationDegrees)
            val labels = labeler.process(image).await()
            
            // Map ML Kit generic labels to our industrial ScrapCategory
            val rawLabels = labels.map { it.text.lowercase() }
            val bestCategory = mapLabelsToCategory(rawLabels)
            val bestConfidence = labels.firstOrNull()?.confidence ?: 0.0f
            
            return ClassificationResult(
                category = bestCategory,
                confidence = bestConfidence,
                rawLabels = labels.map { "${it.text} (${it.confidence})" }
            )
        } catch (e: Exception) {
            return ClassificationResult(ScrapCategory.OTHER, 0f, emptyList())
        }
    }

    private fun mapLabelsToCategory(labels: List<String>): ScrapCategory {
        // Keyword mapping for industrial scrap
        for (label in labels) {
            when {
                label.contains("metal") || label.contains("steel") || label.contains("iron") || label.contains("aluminum") || label.contains("copper") -> return ScrapCategory.METAL
                label.contains("plastic") || label.contains("bottle") || label.contains("pvc") || label.contains("polymer") -> return ScrapCategory.PLASTIC
                label.contains("wood") || label.contains("lumber") || label.contains("timber") || label.contains("pallet") -> return ScrapCategory.WOOD
                label.contains("paper") || label.contains("cardboard") || label.contains("box") || label.contains("carton") -> return ScrapCategory.PAPER
                label.contains("glass") || label.contains("bottle") || label.contains("window") -> return ScrapCategory.GLASS
                label.contains("electronic") || label.contains("computer") || label.contains("phone") || label.contains("circuit") || label.contains("battery") -> return ScrapCategory.EWASTE
                label.contains("oil") || label.contains("chemical") || label.contains("paint") || label.contains("solvent") || label.contains("toxic") -> return ScrapCategory.CHEMICAL
                label.contains("textile") || label.contains("fabric") || label.contains("cloth") || label.contains("garment") || label.contains("cotton") -> return ScrapCategory.OTHER
                label.contains("rubber") || label.contains("tire") || label.contains("latex") -> return ScrapCategory.RUBBER
            }
        }
        return ScrapCategory.OTHER
    }
}
