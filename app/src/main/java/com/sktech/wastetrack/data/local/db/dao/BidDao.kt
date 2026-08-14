package com.sktech.wastetrack.data.local.db.dao

import androidx.room.*
import com.sktech.wastetrack.data.local.db.entity.BidRequestEntity
import com.sktech.wastetrack.data.local.db.entity.BidEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BidDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: BidRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBid(bid: BidEntity)

    @Update
    suspend fun updateRequest(request: BidRequestEntity)

    @Query("SELECT * FROM bid_requests WHERE factoryId = :factoryId ORDER BY auctionStartTime DESC")
    fun getRequestsByFactory(factoryId: String): Flow<List<BidRequestEntity>>

    @Query("SELECT * FROM bid_requests WHERE id = :id")
    suspend fun getRequestById(id: String): BidRequestEntity?

    @Query("SELECT * FROM bid_requests WHERE status = 'OPEN' AND factoryId = :factoryId")
    fun getActiveRequests(factoryId: String): Flow<List<BidRequestEntity>>

    @Query("SELECT * FROM bid_requests WHERE status = 'OPEN' ORDER BY auctionEndTime ASC")
    fun getAllActiveRequests(): Flow<List<BidRequestEntity>>

    @Query("SELECT * FROM bid_requests ORDER BY auctionStartTime DESC")
    fun getAllRequests(): Flow<List<BidRequestEntity>>

    @Query("SELECT * FROM bids WHERE bidRequestId = :requestId ORDER BY pricePerKg DESC")
    fun getBidsByRequest(requestId: String): Flow<List<BidEntity>>

    @Query("UPDATE bids SET isWinning = 0 WHERE bidRequestId = :requestId")
    suspend fun resetWinningForRequest(requestId: String)

    @Query("UPDATE bids SET isWinning = 1 WHERE id = :bidId")
    suspend fun markWinning(bidId: String)

    @Query("SELECT COUNT(*) FROM bid_requests WHERE factoryId = :factoryId AND status = 'OPEN'")
    fun getActiveCount(factoryId: String): Flow<Int>
}

