package com.sktech.wastetrack.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bins")
data class BinEntity(
    @PrimaryKey val id: String,
    val factoryId: String,
    val scrapCategory: String,
    val capacityKg: Float,
    val currentFillKg: Float = 0f,
    val fillPercentage: Float = 0f,
    val predictedFullTimestamp: Long? = null,
    val status: String = "ACTIVE",
    val lastUpdated: Long = System.currentTimeMillis()
)
