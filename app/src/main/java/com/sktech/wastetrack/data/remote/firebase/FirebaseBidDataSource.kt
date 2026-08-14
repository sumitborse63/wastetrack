package com.sktech.wastetrack.data.remote.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.sktech.wastetrack.domain.model.Bid
import com.sktech.wastetrack.domain.model.BidRequest
import com.sktech.wastetrack.domain.model.BidStatus
import com.sktech.wastetrack.domain.model.ScrapCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

class FirebaseBidDataSource @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val bidRequestsCollection = firestore.collection("bid_requests")
    private val bidsCollection = firestore.collection("bids")

    companion object {
        private const val TAG = "FirebaseBidDataSource"
    }

    fun getAllBidRequests(): Flow<List<BidRequest>> {
        return bidRequestsCollection
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        BidRequest(
                            id = doc.id,
                            factoryId = doc.getString("factoryId") ?: "",
                            createdByUserId = doc.getString("createdByUserId") ?: "",
                            scrapEntryId = doc.getString("scrapEntryId") ?: "",
                            scrapCategory = runCatching { ScrapCategory.valueOf(doc.getString("scrapCategory") ?: ScrapCategory.OTHER.name) }.getOrDefault(ScrapCategory.OTHER),
                            estimatedWeightKg = doc.getDouble("estimatedWeightKg")?.toFloat() ?: 0f,
                            reservePricePerKg = doc.getDouble("reservePricePerKg")?.toFloat() ?: 0f,
                            auctionStartTime = doc.getLong("auctionStartTime") ?: 0L,
                            auctionEndTime = doc.getLong("auctionEndTime") ?: 0L,
                            status = runCatching { BidStatus.valueOf(doc.getString("status") ?: BidStatus.OPEN.name) }.getOrDefault(BidStatus.OPEN)
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .catch { e ->
                Log.w(TAG, "Error listening to bid requests: ${e.message}")
                emit(emptyList())
            }
    }

    fun getActiveBidRequests(): Flow<List<BidRequest>> {
        return getAllBidRequests().map { requests ->
            requests.filter { it.status == BidStatus.OPEN }
                .sortedBy { it.auctionEndTime }
        }
    }

    suspend fun createBidRequest(request: BidRequest): String {
        val data = hashMapOf(
            "factoryId" to request.factoryId,
            "createdByUserId" to request.createdByUserId,
            "scrapEntryId" to request.scrapEntryId,
            "scrapCategory" to request.scrapCategory.name,
            "estimatedWeightKg" to request.estimatedWeightKg,
            "reservePricePerKg" to request.reservePricePerKg,
            "auctionStartTime" to request.auctionStartTime,
            "auctionEndTime" to request.auctionEndTime,
            "status" to request.status.name
        )
        bidRequestsCollection.document(request.id).set(data).await()
        return request.id
    }

    fun getBidsForRequest(requestId: String): Flow<List<Bid>> {
        return bidsCollection
            .whereEqualTo("bidRequestId", requestId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        Bid(
                            id = doc.id,
                            bidRequestId = doc.getString("bidRequestId") ?: "",
                            recyclerId = doc.getString("recyclerId") ?: "",
                            recyclerName = doc.getString("recyclerName") ?: "",
                            pricePerKg = doc.getDouble("pricePerKg")?.toFloat() ?: 0f,
                            totalBidAmount = doc.getDouble("totalBidAmount")?.toFloat() ?: 0f,
                            isWinning = doc.getBoolean("isWinning") ?: false,
                            submittedAt = doc.getLong("submittedAt") ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.pricePerKg }
            }
            .catch { e ->
                Log.w(TAG, "Error listening to bids for $requestId: ${e.message}")
                emit(emptyList())
            }
    }

    suspend fun submitBid(bid: Bid): String {
        val data = hashMapOf(
            "bidRequestId" to bid.bidRequestId,
            "recyclerId" to bid.recyclerId,
            "recyclerName" to bid.recyclerName,
            "pricePerKg" to bid.pricePerKg,
            "totalBidAmount" to bid.totalBidAmount,
            "isWinning" to bid.isWinning,
            "submittedAt" to bid.submittedAt
        )
        bidsCollection.document(bid.id).set(data).await()
        return bid.id
    }

    suspend fun awardBid(bidId: String, requestId: String) {
        try {
            // Update all bids for this request so only the winning bid is true
            val query = bidsCollection.whereEqualTo("bidRequestId", requestId).get().await()
            val batch = firestore.batch()
            for (doc in query.documents) {
                val isWinner = doc.id == bidId
                batch.update(doc.reference, "isWinning", isWinner)
            }
            // Update the BidRequest status to AWARDED
            batch.update(bidRequestsCollection.document(requestId), "status", BidStatus.AWARDED.name)
            batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to award bid in Firestore: ${e.message}", e)
        }
    }

    suspend fun getBidRequestById(requestId: String): BidRequest? {
        return try {
            val doc = bidRequestsCollection.document(requestId).get().await()
            if (doc.exists()) {
                BidRequest(
                    id = doc.id,
                    factoryId = doc.getString("factoryId") ?: "",
                    createdByUserId = doc.getString("createdByUserId") ?: "",
                    scrapEntryId = doc.getString("scrapEntryId") ?: "",
                    scrapCategory = runCatching { ScrapCategory.valueOf(doc.getString("scrapCategory") ?: ScrapCategory.OTHER.name) }.getOrDefault(ScrapCategory.OTHER),
                    estimatedWeightKg = doc.getDouble("estimatedWeightKg")?.toFloat() ?: 0f,
                    reservePricePerKg = doc.getDouble("reservePricePerKg")?.toFloat() ?: 0f,
                    auctionStartTime = doc.getLong("auctionStartTime") ?: 0L,
                    auctionEndTime = doc.getLong("auctionEndTime") ?: 0L,
                    status = runCatching { BidStatus.valueOf(doc.getString("status") ?: BidStatus.OPEN.name) }.getOrDefault(BidStatus.OPEN)
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
