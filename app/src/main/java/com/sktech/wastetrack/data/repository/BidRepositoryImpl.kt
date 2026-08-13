package com.sktech.wastetrack.data.repository

import com.sktech.wastetrack.data.local.db.dao.BidDao
import com.sktech.wastetrack.data.remote.firebase.FirebaseBidDataSource
import com.sktech.wastetrack.domain.model.Bid
import com.sktech.wastetrack.domain.model.BidRequest
import com.sktech.wastetrack.domain.repository.IBidRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BidRepositoryImpl @Inject constructor(
    private val bidDao: BidDao,
    private val firebaseBidDataSource: FirebaseBidDataSource
) : IBidRepository {

    override fun getActiveBidRequests(): Flow<List<BidRequest>> {
        // For pilot, fetch directly from Firebase for real-time
        return firebaseBidDataSource.getActiveBidRequests()
    }

    override suspend fun createBidRequest(request: BidRequest): String {
        // Save to Firebase
        val id = firebaseBidDataSource.createBidRequest(request)
        // You could also save to local Room for offline caching here
        return id
    }

    override fun getBidsForRequest(requestId: String): Flow<List<Bid>> {
        // Listen to Firebase directly for real-time updates
        return firebaseBidDataSource.getBidsForRequest(requestId)
    }

    override suspend fun submitBid(bid: Bid): String {
        return firebaseBidDataSource.submitBid(bid)
    }

    override suspend fun awardBid(bidId: String, requestId: String) {
        firebaseBidDataSource.awardBid(bidId, requestId)
    }
}
