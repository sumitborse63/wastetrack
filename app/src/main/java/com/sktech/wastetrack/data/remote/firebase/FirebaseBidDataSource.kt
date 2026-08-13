package com.sktech.wastetrack.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.sktech.wastetrack.domain.model.Bid
import com.sktech.wastetrack.domain.model.BidRequest
import com.sktech.wastetrack.domain.model.BidStatus
import com.sktech.wastetrack.domain.model.ScrapCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

class FirebaseBidDataSource @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val bidRequestsCollection = firestore.collection("bidRequests")
    private val bidsCollection = firestore.collection("bids")

    fun getActiveBidRequests(): Flow<List<BidRequest>> {
        return bidRequestsCollection
            .whereEqualTo("status", BidStatus.OPEN.name)
            .orderBy("auctionEndTime", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        BidRequest(
                            id = doc.id,
                            factoryId = doc.getString("factoryId") ?: "",
                            scrapEntryId = doc.getString("scrapEntryId") ?: "",
                            scrapCategory = ScrapCategory.valueOf(doc.getString("scrapCategory") ?: ScrapCategory.OTHER.name),
                            estimatedWeightKg = doc.getDouble("estimatedWeightKg")?.toFloat() ?: 0f,
                            reservePricePerKg = doc.getDouble("reservePricePerKg")?.toFloat() ?: 0f,
                            auctionStartTime = doc.getLong("auctionStartTime") ?: 0L,
                            auctionEndTime = doc.getLong("auctionEndTime") ?: 0L,
                            status = BidStatus.valueOf(doc.getString("status") ?: BidStatus.OPEN.name)
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    suspend fun createBidRequest(request: BidRequest): String {
        val data = hashMapOf(
            "factoryId" to request.factoryId,
            "scrapEntryId" to request.scrapEntryId,
            "scrapCategory" to request.scrapCategory.name,
            "estimatedWeightKg" to request.estimatedWeightKg,
            "reservePricePerKg" to request.reservePricePerKg,
            "auctionStartTime" to request.auctionStartTime,
            "auctionEndTime" to request.auctionEndTime,
            "status" to request.status.name
        )
        val docRef = bidRequestsCollection.add(data).await()
        return docRef.id
    }

    fun getBidsForRequest(requestId: String): Flow<List<Bid>> {
        return bidsCollection
            .whereEqualTo("bidRequestId", requestId)
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        Bid(
                            id = doc.id,
                            bidRequestId = doc.getString("bidRequestId") ?: "",
                            recyclerId = doc.getString("recyclerId") ?: "",
                            pricePerKg = doc.getDouble("pricePerKg")?.toFloat() ?: 0f,
                            totalBidAmount = doc.getDouble("totalBidAmount")?.toFloat() ?: 0f,
                            isWinning = doc.getBoolean("isWinning") ?: false,
                            submittedAt = doc.getLong("submittedAt") ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    suspend fun submitBid(bid: Bid): String {
        val data = hashMapOf(
            "bidRequestId" to bid.bidRequestId,
            "recyclerId" to bid.recyclerId,
            "pricePerKg" to bid.pricePerKg,
            "totalBidAmount" to bid.totalBidAmount,
            "isWinning" to bid.isWinning,
            "submittedAt" to bid.submittedAt
        )
        val docRef = bidsCollection.add(data).await()
        return docRef.id
    }

    suspend fun awardBid(bidId: String, requestId: String) {
        // Mark the selected bid as winning
        bidsCollection.document(bidId).update("isWinning", true).await()
        
        // Update the BidRequest status to AWARDED
        val querySnapshot = bidRequestsCollection.whereEqualTo("id", requestId).get().await()
        if (!querySnapshot.isEmpty) {
            val docId = querySnapshot.documents[0].id
            bidRequestsCollection.document(docId).update("status", BidStatus.AWARDED.name).await()
        }
    }
}
