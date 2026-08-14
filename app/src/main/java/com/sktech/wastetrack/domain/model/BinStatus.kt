package com.sktech.wastetrack.domain.model

import com.sktech.wastetrack.R

enum class BinStatus(val displayName: String, val nameRes: Int) {
    ACTIVE("Active", R.string.bin_status_active),
    FULL("Full", R.string.bin_status_full),
    DISPATCHED("Dispatched", R.string.bin_status_dispatched),
    MAINTENANCE("Maintenance", R.string.bin_status_maintenance)
}

