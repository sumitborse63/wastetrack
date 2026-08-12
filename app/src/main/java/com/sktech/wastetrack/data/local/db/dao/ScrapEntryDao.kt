package com.sktech.wastetrack.data.local.db.dao

import androidx.room.*
import com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrapEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ScrapEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<ScrapEntryEntity>)

    @Update
    suspend fun update(entry: ScrapEntryEntity)

    @Query("SELECT * FROM scrap_entries WHERE factoryId = :factoryId ORDER BY createdAt DESC")
    fun getByFactory(factoryId: String): Flow<List<ScrapEntryEntity>>

    @Query("SELECT * FROM scrap_entries WHERE factoryId = :factoryId AND category = :category ORDER BY createdAt DESC")
    fun getByCategory(factoryId: String, category: String): Flow<List<ScrapEntryEntity>>

    @Query("SELECT * FROM scrap_entries WHERE id = :id")
    suspend fun getById(id: String): ScrapEntryEntity?

    @Query("SELECT * FROM scrap_entries WHERE syncStatus = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingSync(): List<ScrapEntryEntity>

    @Query("UPDATE scrap_entries SET syncStatus = 'SYNCED', syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM scrap_entries WHERE factoryId = :factoryId")
    fun getCount(factoryId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM scrap_entries WHERE factoryId = :factoryId AND createdAt >= :since")
    fun getCountSince(factoryId: String, since: Long): Flow<Int>

    @Query("SELECT SUM(weightKg) FROM scrap_entries WHERE factoryId = :factoryId AND createdAt >= :since")
    fun getTotalWeightSince(factoryId: String, since: Long): Flow<Float?>

    @Query("SELECT * FROM scrap_entries WHERE anomalyFlagged = 1 AND factoryId = :factoryId ORDER BY createdAt DESC")
    fun getAnomalies(factoryId: String): Flow<List<ScrapEntryEntity>>

    @Delete
    suspend fun delete(entry: ScrapEntryEntity)
}
