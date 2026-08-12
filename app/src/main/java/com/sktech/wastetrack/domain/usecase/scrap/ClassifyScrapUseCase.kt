package com.sktech.wastetrack.domain.usecase.scrap

import android.graphics.Bitmap
import com.sktech.wastetrack.data.ml.ClassificationResult
import com.sktech.wastetrack.data.ml.ScrapClassifier
import javax.inject.Inject

class ClassifyScrapUseCase @Inject constructor(
    private val classifier: ScrapClassifier
) {
    suspend operator fun invoke(bitmap: Bitmap, rotationDegrees: Int): ClassificationResult {
        return classifier.classifyImage(bitmap, rotationDegrees)
    }
}
