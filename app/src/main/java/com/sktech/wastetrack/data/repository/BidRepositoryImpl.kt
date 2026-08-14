package com.sktech.wastetrack.data.repository

import com.sktech.wastetrack.data.local.db.dao.BidDao
import com.sktech.wastetrack.data.local.db.entity.BidEntity
import com.sktech.wastetrack.data.local.db.entity.BidRequestEntity
import com.sktech.wastetrack.data.remote.firebase.FirebaseBidDataSource
import com.sktech.wastetrack.domain.model.Bid
import com.sktech.wastetrack.domain.model.BidRequest
import com.sktech.wastetrack.domain.model.BidStatus
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.domain.repository.IBidRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class BidRepositoryImpl @Inject constructor(
    private val bidDao: BidDao,
    private val firebaseBidDataSource: FirebaseBidDataSource
) : IBidRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun getActiveBidRequests(): Flow<List<BidRequest>> {
        val roomFlow = bidDao.getAllActiveRequests().map { entities ->
            entities.map { entity ->
                BidRequest(
                    id = entity.id,
                    factoryId = entity.factoryId,
                    createdByUserId = entity.createdByUserId,
                    scrapEntryId = entity.scrapEntryId,
                    scrapCategory = runCatching { ScrapCategory.valueOf(entity.scrapCategory) }.getOrDefault(ScrapCategory.OTHER),
                    estimatedWeightKg = entity.estimatedWeightKg,
                    reservePricePerKg = entity.reservePricePerKg,
                    auctionStartTime = entity.auctionStartTime,
                    auctionEndTime = entity.auctionEndTime,
                    status = runCatching { BidStatus.valueOf(entity.status) }.getOrDefault(BidStatus.OPEN)
                )
            }
        }

        val firebaseFlow = firebaseBidDataSource.getActiveBidRequests()
            .catch { emit(emptyList()) }
            .onStart { emit(emptyList()) }

        return combine(roomFlow, firebaseFlow) { local, remote ->
            // Background sync remote requests to local Room without overwriting local AWARDED state
            scope.launch {
                remote.forEach { r ->
                    val localReq = bidDao.getRequestById(r.id)
                    if (localReq == null || localReq.status != "AWARDED") {
                        bidDao.insertRequest(
                            BidRequestEntity(
                                id = r.id,
                                factoryId = r.factoryId,
                                createdByUserId = r.createdByUserId,
                                scrapEntryId = r.scrapEntryId,
                                scrapCategory = r.scrapCategory.name,
                                estimatedWeightKg = r.estimatedWeightKg,
                                reservePricePerKg = r.reservePricePerKg,
                                auctionStartTime = r.auctionStartTime,
                                auctionEndTime = r.auctionEndTime,
                                status = r.status.name
                            )
                        )
                    }
                }
            }
            val localMap = local.associateBy { it.id }
            val remoteMap = remote.associateBy { it.id }
            val merged = (remote.map { r ->
                val l = localMap[r.id]
                if (l != null && l.status == BidStatus.AWARDED) r.copy(status = BidStatus.AWARDED) else r
            } + local.filter { !remoteMap.containsKey(it.id) })
                .sortedBy { it.auctionEndTime }
            merged
        }
    }

    override fun getAllBidRequests(): Flow<List<BidRequest>> {
        val roomFlow = bidDao.getAllRequests().map { entities ->
            entities.map { entity ->
                BidRequest(
                    id = entity.id,
                    factoryId = entity.factoryId,
                    createdByUserId = entity.createdByUserId,
                    scrapEntryId = entity.scrapEntryId,
                    scrapCategory = runCatching { ScrapCategory.valueOf(entity.scrapCategory) }.getOrDefault(ScrapCategory.OTHER),
                    estimatedWeightKg = entity.estimatedWeightKg,
                    reservePricePerKg = entity.reservePricePerKg,
                    auctionStartTime = entity.auctionStartTime,
                    auctionEndTime = entity.auctionEndTime,
                    status = runCatching { BidStatus.valueOf(entity.status) }.getOrDefault(BidStatus.OPEN)
                )
            }
        }

        val firebaseFlow = firebaseBidDataSource.getAllBidRequests()
            .catch { emit(emptyList()) }
            .onStart { emit(emptyList()) }

        return combine(roomFlow, firebaseFlow) { local, remote ->
            scope.launch {
                remote.forEach { r ->
                    val localReq = bidDao.getRequestById(r.id)
                    if (localReq == null || localReq.status != "AWARDED") {
                        bidDao.insertRequest(
                            BidRequestEntity(
                                id = r.id,
                                factoryId = r.factoryId,
                                createdByUserId = r.createdByUserId,
                                scrapEntryId = r.scrapEntryId,
                                scrapCategory = r.scrapCategory.name,
                                estimatedWeightKg = r.estimatedWeightKg,
                                reservePricePerKg = r.reservePricePerKg,
                                auctionStartTime = r.auctionStartTime,
                                auctionEndTime = r.auctionEndTime,
                                status = r.status.name
                            )
                        )
                    }
                }
            }
            val localMap = local.associateBy { it.id }
            val remoteMap = remote.associateBy { it.id }
            val merged = (remote.map { r ->
                val l = localMap[r.id]
                if (l != null && l.status == BidStatus.AWARDED) r.copy(status = BidStatus.AWARDED) else r
            } + local.filter { !remoteMap.containsKey(it.id) })
                .sortedByDescending { it.auctionStartTime }
            merged
        }
    }

    override suspend fun createBidRequest(request: BidRequest): String {
        // 1. Immediately cache in local Room DB for 0ms latency
        bidDao.insertRequest(
            BidRequestEntity(
                id = request.id,
                factoryId = request.factoryId,
                createdByUserId = request.createdByUserId,
                scrapEntryId = request.scrapEntryId,
                scrapCategory = request.scrapCategory.name,
                estimatedWeightKg = request.estimatedWeightKg,
                reservePricePerKg = request.reservePricePerKg,
                auctionStartTime = request.auctionStartTime,
                auctionEndTime = request.auctionEndTime,
                status = request.status.name
            )
        )

        // 2. Sync to Firebase asynchronously
        scope.launch {
            try {
                withTimeoutOrNull(5000L) {
                    firebaseBidDataSource.createBidRequest(request)
                }
            } catch (e: Exception) {
                // Will sync when online
            }
        }

        return request.id
    }

    override fun getBidsForRequest(requestId: String): Flow<List<Bid>> {
        val roomFlow = bidDao.getBidsByRequest(requestId).map { entities ->
            entities.map { entity ->
                Bid(
                    id = entity.id,
                    bidRequestId = entity.bidRequestId,
                    recyclerId = entity.recyclerId,
                    recyclerName = entity.recyclerName,
                    pricePerKg = entity.pricePerKg,
                    totalBidAmount = entity.totalBidAmount,
                    isWinning = entity.isWinning,
                    submittedAt = entity.submittedAt
                )
            }
        }

        val firebaseFlow = firebaseBidDataSource.getBidsForRequest(requestId)
            .catch { emit(emptyList()) }
            .onStart { emit(emptyList()) }

        return combine(roomFlow, firebaseFlow) { local, remote ->
            // Save any newly arrived remote bids to Room unless locally awarded
            scope.launch {
                val localReq = bidDao.getRequestById(requestId)
                val isLocallyAwarded = localReq?.status == "AWARDED"
                remote.forEach { b ->
                    if (!isLocallyAwarded || b.isWinning) {
                        bidDao.insertBid(
                            BidEntity(
                                id = b.id,
                                bidRequestId = b.bidRequestId,
                                recyclerId = b.recyclerId,
                                recyclerName = b.recyclerName,
                                pricePerKg = b.pricePerKg,
                                totalBidAmount = b.totalBidAmount,
                                isWinning = b.isWinning,
                                submittedAt = b.submittedAt
                            )
                        )
                    }
                }
            }
            val localMap = local.associateBy { it.id }
            val remoteMap = remote.associateBy { it.id }
            val merged = (remote.map { r ->
                val l = localMap[r.id]
                if (l != null && l.isWinning) r.copy(isWinning = true) else r
            } + local.filter { !remoteMap.containsKey(it.id) })
                .sortedByDescending { it.pricePerKg }
            merged
        }
    }

    override suspend fun submitBid(bid: Bid): String {
        // 1. Immediately cache in Room for instant UI reactivity
        bidDao.insertBid(
            BidEntity(
                id = bid.id,
                bidRequestId = bid.bidRequestId,
                recyclerId = bid.recyclerId,
                recyclerName = bid.recyclerName,
                pricePerKg = bid.pricePerKg,
                totalBidAmount = bid.totalBidAmount,
                isWinning = bid.isWinning,
                submittedAt = bid.submittedAt
            )
        )

        // 2. Sync to Firebase
        scope.launch {
            try {
                withTimeoutOrNull(5000L) {
                    firebaseBidDataSource.submitBid(bid)
                }
            } catch (e: Exception) {
                // Will sync when online
            }
        }

        return bid.id
    }

    override suspend fun awardBid(bidId: String, requestId: String) {
        bidDao.resetWinningForRequest(requestId)
        bidDao.markWinning(bidId)
        val req = bidDao.getRequestById(requestId)
        if (req != null) {
            bidDao.updateRequest(req.copy(status = "AWARDED"))
        }

        scope.launch {
            try {
                withTimeoutOrNull(5000L) {
                    firebaseBidDataSource.awardBid(bidId, requestId)
                }
            } catch (e: Exception) {
                // Fallback
            }
        }
    }

    override suspend fun getBidRequestById(requestId: String): BidRequest? {
        val local = bidDao.getRequestById(requestId)
        if (local != null) {
            return BidRequest(
                id = local.id,
                factoryId = local.factoryId,
                createdByUserId = local.createdByUserId,
                scrapEntryId = local.scrapEntryId,
                scrapCategory = runCatching { ScrapCategory.valueOf(local.scrapCategory) }.getOrDefault(ScrapCategory.OTHER),
                estimatedWeightKg = local.estimatedWeightKg,
                reservePricePerKg = local.reservePricePerKg,
                auctionStartTime = local.auctionStartTime,
                auctionEndTime = local.auctionEndTime,
                status = runCatching { BidStatus.valueOf(local.status) }.getOrDefault(BidStatus.OPEN)
            )
        }
        return firebaseBidDataSource.getBidRequestById(requestId)
    }
}
