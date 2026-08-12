package com.sktech.wastetrack.domain.model

enum class BinStatus(val displayName: String) {
    ACTIVE("Active"),
    FULL("Full"),
    DISPATCHED("Dispatched"),
    MAINTENANCE("Maintenance")
}
