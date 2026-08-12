package com.sktech.wastetrack.data.ml

import com.sktech.wastetrack.domain.model.ScrapCategory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeEstimator @Inject constructor() {

    /**
     * Estimates if the given weight is anomalous for the reported category and volume.
     * In a real application, this would use depth-sensing (ARCore) or bounding box areas
     * to estimate the true volume of the pile. 
     * 
     * For this MVP, we use statistical thresholds based on typical material densities
     * (kg per cubic meter) to flag severe deviations.
     */
    fun detectAnomaly(category: ScrapCategory, reportedWeightKg: Float, estimatedVolumeM3: Float = 1.0f): AnomalyResult {
        // Typical bulk densities in kg/m^3 (approximate ranges for loose scrap)
        val densityRange = when (category) {
            ScrapCategory.METAL -> 400f..1500f
            ScrapCategory.PLASTIC -> 50f..250f
            ScrapCategory.WOOD -> 200f..600f
            ScrapCategory.PAPER -> 30f..80f
            ScrapCategory.GLASS -> 200f..800f
            ScrapCategory.EWASTE -> 50f..300f
            ScrapCategory.CHEMICAL -> 800f..1200f
            ScrapCategory.RUBBER -> 100f..500f
            ScrapCategory.OTHER -> 10f..1000f
        }

        // Calculate reported density
        // If no volume is provided (e.g. manual entry without AR measurement), we assume 1m^3 
        // and just flag if the weight is extremely outside normal bounds for a typical bin load.
        val reportedDensity = reportedWeightKg / estimatedVolumeM3

        val isTooHeavy = reportedDensity > densityRange.endInclusive * 1.5f // 50% tolerance
        val isTooLight = reportedDensity < densityRange.start * 0.5f

        val isAnomalous = isTooHeavy || isTooLight
        
        // Score from 0.0 (normal) to 1.0 (highly anomalous)
        val score = if (!isAnomalous) {
            0.1f
        } else if (isTooHeavy) {
            (reportedDensity / densityRange.endInclusive).coerceAtMost(1.0f)
        } else {
            (densityRange.start / (reportedDensity + 0.1f)).coerceAtMost(1.0f)
        }

        val reason = when {
            isTooHeavy -> "Weight suspiciously high for this material volume. Possible fraud (added ballast)."
            isTooLight -> "Weight suspiciously low. Check if material category is correct."
            else -> "Weight is within expected bounds."
        }

        return AnomalyResult(isAnomalous, score, reason)
    }
}

data class AnomalyResult(
    val flagged: Boolean,
    val score: Float,
    val reason: String
)
