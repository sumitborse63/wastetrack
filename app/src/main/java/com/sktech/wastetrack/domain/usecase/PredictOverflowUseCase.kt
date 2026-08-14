package com.sktech.wastetrack.domain.usecase

import com.sktech.wastetrack.domain.model.Bin
import javax.inject.Inject

class PredictOverflowUseCase @Inject constructor() {
    
    /**
     * Estimates when a bin will reach capacity using a conservative category baseline.
     * A future server/edge model can replace these rates when enough historical fill events
     * have been collected, but this estimator is deterministic and works offline today.
     */
    operator fun invoke(bin: Bin): Long? {
        if (bin.fillPercentage >= 100f) {
            return System.currentTimeMillis()
        }
        
        if (bin.fillPercentage == 0f) {
            return null // Not enough data to predict
        }

        // Baseline generation rate in kg/hour for initial offline operation.
        val generationRateKgPerHour = when (bin.scrapCategory) {
            com.sktech.wastetrack.domain.model.ScrapCategory.METAL -> 50f
            com.sktech.wastetrack.domain.model.ScrapCategory.PAPER -> 30f
            com.sktech.wastetrack.domain.model.ScrapCategory.PLASTIC -> 10f
            else -> 20f
        }

        val remainingCapacityKg = bin.capacityKg - bin.currentFillKg
        if (remainingCapacityKg <= 0) return System.currentTimeMillis()

        val hoursUntilFull = remainingCapacityKg / generationRateKgPerHour
        
        // Convert hours to milliseconds
        val msUntilFull = (hoursUntilFull * 60 * 60 * 1000).toLong()
        
        return System.currentTimeMillis() + msUntilFull
    }
}
