package com.sktech.wastetrack.domain.model

import com.sktech.wastetrack.R

enum class BidStatus(val displayName: String, val nameRes: Int) {
    OPEN("Open", R.string.bid_status_open),
    CLOSED("Closed", R.string.bid_status_closed),
    AWARDED("Awarded", R.string.bid_status_awarded),
    EXPIRED("Expired", R.string.bid_status_expired)
}

