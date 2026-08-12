package com.sktech.wastetrack.domain.model

data class ScrapEntry(
    val id: String,
    val factoryId: String,
    val loggedByUserId: String,
    val category: ScrapCategory,
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
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val contentHash: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)
