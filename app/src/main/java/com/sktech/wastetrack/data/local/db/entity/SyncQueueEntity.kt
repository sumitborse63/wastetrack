package com.sktech.wastetrack.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: String,
    val action: String, // CREATE, UPDATE, DELETE
    val payload: String, // JSON serialized entity
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
