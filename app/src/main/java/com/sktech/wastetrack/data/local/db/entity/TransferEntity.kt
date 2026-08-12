package com.sktech.wastetrack.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey val id: String,
    val scrapEntryId: String,
    val fromFactoryId: String,
    val toRecyclerId: String? = null,
    val supervisorId: String,
    val driverId: String? = null,
    val weightAtSource: Float,
    val weightAtDestination: Float? = null,
    val weightDiscrepancy: Float? = null,
    val status: String = "INITIATED",
    val vehicleNumber: String = "",
    val gatePassNumber: String = "",
    val syncStatus: String = "PENDING",
    val contentHash: String = "",
    val initiatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
