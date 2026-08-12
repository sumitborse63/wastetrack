package com.sktech.wastetrack.domain.model

data class QRHandshake(
    val id: String,
    val transferId: String,
    val qrPayload: String,
    val supervisorSignature: String = "",
    val driverSignature: String = "",
    val generatedAt: Long = System.currentTimeMillis(),
    val scannedAt: Long? = null,
    val isValid: Boolean = false
)
