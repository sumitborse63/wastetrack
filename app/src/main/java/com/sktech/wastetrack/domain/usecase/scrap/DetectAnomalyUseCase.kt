package com.sktech.wastetrack.domain.usecase.scrap

import com.sktech.wastetrack.data.ml.AnomalyResult
import com.sktech.wastetrack.data.ml.VolumeEstimator
import com.sktech.wastetrack.domain.model.ScrapCategory
import javax.inject.Inject

class DetectAnomalyUseCase @Inject constructor(
    private val estimator: VolumeEstimator
) {
    operator fun invoke(category: ScrapCategory, weightKg: Float): AnomalyResult {
        // Without an AR volume estimator, we pass default 1.0m3 for density checks
        return estimator.detectAnomaly(category, weightKg)
    }
}
