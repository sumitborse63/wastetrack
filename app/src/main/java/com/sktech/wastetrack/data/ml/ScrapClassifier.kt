package com.sktech.wastetrack.data.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.common.model.LocalModel
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
    // Load the custom MobileNet V1 model from assets
    private val localModel = LocalModel.Builder()
        .setAssetFilePath("mobilenet_v1_1.0_224_quant.tflite")
        .build()

    private val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
        .setConfidenceThreshold(0.2f)
        .setMaxResultCount(5)
        .build()

    private val labeler = ImageLabeling.getClient(customImageLabelerOptions)

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
                label.contains("metal") || label.contains("steel") || label.contains("iron") || label.contains("aluminum") || label.contains("copper") || label.contains("tin") || label.contains("can") || label.contains("wire") || label.contains("pipe") -> return ScrapCategory.METAL
                label.contains("plastic") || label.contains("bottle") || label.contains("pvc") || label.contains("polymer") || label.contains("container") || label.contains("cup") || label.contains("toy") -> return ScrapCategory.PLASTIC
                label.contains("wood") || label.contains("lumber") || label.contains("timber") || label.contains("pallet") || label.contains("board") -> return ScrapCategory.WOOD
                label.contains("paper") || label.contains("cardboard") || label.contains("box") || label.contains("carton") || label.contains("newspaper") || label.contains("book") -> return ScrapCategory.PAPER
                label.contains("glass") || label.contains("window") || label.contains("jar") -> return ScrapCategory.GLASS
                label.contains("electronic") || label.contains("computer") || label.contains("phone") || label.contains("circuit") || label.contains("battery") || label.contains("appliance") || label.contains("cable") -> return ScrapCategory.EWASTE
                label.contains("oil") || label.contains("chemical") || label.contains("paint") || label.contains("solvent") || label.contains("toxic") -> return ScrapCategory.CHEMICAL
                label.contains("textile") || label.contains("fabric") || label.contains("cloth") || label.contains("garment") || label.contains("cotton") -> return ScrapCategory.OTHER
                label.contains("rubber") || label.contains("tire") || label.contains("latex") -> return ScrapCategory.RUBBER
            }
        }
        return ScrapCategory.OTHER
    }
}
