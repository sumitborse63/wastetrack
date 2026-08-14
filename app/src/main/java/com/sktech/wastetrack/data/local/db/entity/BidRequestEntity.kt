package com.sktech.wastetrack.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bid_requests")
data class BidRequestEntity(
    @PrimaryKey val id: String,
    val factoryId: String,
    val createdByUserId: String = "",
    val scrapEntryId: String,
    val scrapCategory: String,
    val estimatedWeightKg: Float,
    val reservePricePerKg: Float = 0f,
    val auctionStartTime: Long = System.currentTimeMillis(),
    val auctionEndTime: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000,
    val status: String = "OPEN"
)
