package com.sktech.wastetrack.domain.model

enum class TransferStatus(val displayName: String) {
    INITIATED("Initiated"),
    QR_GENERATED("QR Generated"),
    QR_SCANNED("QR Scanned"),
    IN_TRANSIT("In Transit"),
    DELIVERED("Delivered"),
    VERIFIED("Verified"),
    DISPUTED("Disputed")
}
