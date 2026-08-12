package com.sktech.wastetrack.domain.model

enum class BidStatus(val displayName: String) {
    OPEN("Open"),
    CLOSED("Closed"),
    AWARDED("Awarded"),
    EXPIRED("Expired")
}
