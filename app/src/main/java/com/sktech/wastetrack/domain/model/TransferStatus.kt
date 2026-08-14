package com.sktech.wastetrack.domain.model

import com.sktech.wastetrack.R

enum class TransferStatus(val displayName: String, val nameRes: Int) {
    INITIATED("Initiated", R.string.status_initiated),
    QR_GENERATED("QR Generated", R.string.status_qr_generated),
    QR_SCANNED("QR Scanned", R.string.status_qr_scanned),
    IN_TRANSIT("In Transit", R.string.status_in_transit),
    DELIVERED("Delivered", R.string.status_delivered),
    VERIFIED("Verified", R.string.status_verified),
    DISPUTED("Disputed", R.string.status_disputed)
}

