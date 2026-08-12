package com.sktech.wastetrack.domain.model

data class Bin(
    val id: String,
    val factoryId: String,
    val scrapCategory: ScrapCategory,
    val capacityKg: Float,
    val currentFillKg: Float = 0f,
    val fillPercentage: Float = 0f,
    val predictedFullTimestamp: Long? = null,
    val status: BinStatus = BinStatus.ACTIVE,
    val lastUpdated: Long = System.currentTimeMillis()
)
