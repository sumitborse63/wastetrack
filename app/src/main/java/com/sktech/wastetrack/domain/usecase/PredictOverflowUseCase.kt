package com.sktech.wastetrack.domain.usecase

import com.sktech.wastetrack.domain.model.Bin
import javax.inject.Inject

class PredictOverflowUseCase @Inject constructor() {
    
    /**
     * Simulates an ML prediction of when the bin will overflow.
     * In a production app, this would use a TFLite model trained on historical fill rates,
     * shift times, and factory production schedules.
     * 
     * For the MVP, we use a heuristic based on current fill percentage 
     * and an assumed generation rate per hour.
     */
    operator fun invoke(bin: Bin): Long? {
        if (bin.fillPercentage >= 100f) {
            return System.currentTimeMillis()
        }
        
        if (bin.fillPercentage == 0f) {
            return null // Not enough data to predict
        }

        // Mock ML calculation:
        // Assume factory generates X kg per hour based on category.
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
