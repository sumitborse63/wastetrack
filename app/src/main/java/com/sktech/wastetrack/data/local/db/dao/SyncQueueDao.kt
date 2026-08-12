package com.sktech.wastetrack.data.local.db.dao

import androidx.room.*
import com.sktech.wastetrack.data.local.db.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert
    suspend fun enqueue(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT :limit")
    suspend fun dequeueOldest(limit: Int = 50): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_queue WHERE entityId = :entityId AND entityType = :entityType")
    suspend fun deleteByEntity(entityId: String, entityType: String)

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    @Query("DELETE FROM sync_queue WHERE retryCount > :maxRetries")
    suspend fun clearFailed(maxRetries: Int = 5)

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getCount(): Flow<Int>
}
