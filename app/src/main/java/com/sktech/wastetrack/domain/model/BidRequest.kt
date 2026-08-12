package com.sktech.wastetrack.domain.model

data class BidRequest(
    val id: String,
    val factoryId: String,
    val scrapEntryId: String,
    val scrapCategory: String,
    val estimatedWeightKg: Float,
    val reservePricePerKg: Float,
    val auctionStartTime: Long,
    val auctionEndTime: Long,
    val status: BidStatus
)
