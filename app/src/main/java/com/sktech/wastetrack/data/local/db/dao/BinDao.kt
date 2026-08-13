package com.sktech.wastetrack.data.local.db.dao

import androidx.room.*
import com.sktech.wastetrack.data.local.db.entity.BinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BinDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bin: BinEntity)

    @Update
    suspend fun update(bin: BinEntity)

    @Query("SELECT * FROM bins WHERE factoryId = :factoryId ORDER BY fillPercentage DESC")
    fun getByFactory(factoryId: String): Flow<List<BinEntity>>

    @Query("SELECT * FROM bins WHERE id = :id")
    suspend fun getById(id: String): BinEntity?

    @Query("UPDATE bins SET currentFillKg = :fillKg, fillPercentage = :fillPct, predictedFullTimestamp = :predictedFullTimestamp, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateFillLevel(id: String, fillKg: Float, fillPct: Float, predictedFullTimestamp: Long?, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM bins WHERE factoryId = :factoryId AND fillPercentage >= :threshold ORDER BY fillPercentage DESC")
    fun getOverflowRisk(factoryId: String, threshold: Float = 85f): Flow<List<BinEntity>>

    @Query("SELECT COUNT(*) FROM bins WHERE factoryId = :factoryId AND fillPercentage >= :threshold")
    fun getAlertCount(factoryId: String, threshold: Float = 85f): Flow<Int>

    @Delete
    suspend fun delete(bin: BinEntity)
}
