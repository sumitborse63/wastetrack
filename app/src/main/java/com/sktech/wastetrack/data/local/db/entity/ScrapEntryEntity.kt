package com.sktech.wastetrack.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scrap_entries")
data class ScrapEntryEntity(
    @PrimaryKey val id: String,
    val factoryId: String,
    val loggedByUserId: String,
    val category: String,
    val subCategory: String = "",
    val weightKg: Float,
    val estimatedVolumeL: Float = 0f,
    val anomalyScore: Float = 0f,
    val anomalyFlagged: Boolean = false,
    val imageUri: String? = null,
    val mlClassificationResult: String? = null,
    val mlConfidence: Float = 0f,
    val voiceNoteUri: String? = null,
    val notes: String = "",
    val syncStatus: String = "PENDING",
    val contentHash: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)
