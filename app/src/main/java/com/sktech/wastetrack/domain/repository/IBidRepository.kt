package com.sktech.wastetrack.domain.repository

import com.sktech.wastetrack.domain.model.Bid
import com.sktech.wastetrack.domain.model.BidRequest
import kotlinx.coroutines.flow.Flow

interface IBidRepository {
    fun getActiveBidRequests(): Flow<List<BidRequest>>
    fun getAllBidRequests(): Flow<List<BidRequest>>
    suspend fun createBidRequest(request: BidRequest): String
    fun getBidsForRequest(requestId: String): Flow<List<Bid>>
    suspend fun submitBid(bid: Bid): String
    suspend fun awardBid(bidId: String, requestId: String)
    suspend fun getBidRequestById(requestId: String): BidRequest?
}

