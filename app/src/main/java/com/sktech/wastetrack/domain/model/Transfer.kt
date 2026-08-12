package com.sktech.wastetrack.domain.model

data class Transfer(
    val id: String,
    val scrapEntryId: String,
    val fromFactoryId: String,
    val toRecyclerId: String? = null,
    val supervisorId: String,
    val driverId: String? = null,
    val weightAtSource: Float,
    val weightAtDestination: Float? = null,
    val weightDiscrepancy: Float? = null,
    val status: TransferStatus = TransferStatus.INITIATED,
    val vehicleNumber: String = "",
    val gatePassNumber: String = "",
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val contentHash: String = "",
    val initiatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
