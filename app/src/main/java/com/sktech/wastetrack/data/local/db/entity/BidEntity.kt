package com.sktech.wastetrack.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bids")
data class BidEntity(
    @PrimaryKey val id: String,
    val bidRequestId: String,
    val recyclerId: String,
    val recyclerName: String = "",
    val pricePerKg: Float,
    val totalBidAmount: Float,
    val isWinning: Boolean = false,
    val submittedAt: Long = System.currentTimeMillis()
)
