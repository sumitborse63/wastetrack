package com.sktech.wastetrack.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qr_handshakes")
data class QRHandshakeEntity(
    @PrimaryKey val id: String,
    val transferId: String,
    val qrPayload: String,
    val supervisorSignature: String = "",
    val driverSignature: String = "",
    val generatedAt: Long = System.currentTimeMillis(),
    val scannedAt: Long? = null,
    val isValid: Boolean = false
)
