package com.sktech.wastetrack.domain.model

data class BidRequest(
    val id: String,
    val factoryId: String,
    val createdByUserId: String = "",
    val scrapEntryId: String,
    val scrapCategory: ScrapCategory,
    val estimatedWeightKg: Float,
    val reservePricePerKg: Float = 0f,
    val auctionStartTime: Long = System.currentTimeMillis(),
    val auctionEndTime: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000, // 24h
    val status: BidStatus = BidStatus.OPEN,
    val bids: List<Bid> = emptyList()
)

data class Bid(
    val id: String,
    val bidRequestId: String,
    val recyclerId: String,
    val recyclerName: String = "",
    val pricePerKg: Float,
    val totalBidAmount: Float,
    val isWinning: Boolean = false,
    val submittedAt: Long = System.currentTimeMillis()
)
