package com.sktech.wastetrack.data.local.db.dao

import androidx.room.*
import com.sktech.wastetrack.data.local.db.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transfer: TransferEntity)

    @Update
    suspend fun update(transfer: TransferEntity)

    @Query("SELECT * FROM transfers WHERE fromFactoryId = :factoryId ORDER BY initiatedAt DESC")
    fun getByFactory(factoryId: String): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun getById(id: String): TransferEntity?

    @Query("UPDATE transfers SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT * FROM transfers WHERE syncStatus = 'PENDING' ORDER BY initiatedAt ASC")
    suspend fun getPendingSync(): List<TransferEntity>

    @Query("UPDATE transfers SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COUNT(*) FROM transfers WHERE fromFactoryId = :factoryId AND status = :status")
    fun getCountByStatus(factoryId: String, status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM transfers WHERE fromFactoryId = :factoryId AND initiatedAt >= :since")
    fun getCountSince(factoryId: String, since: Long): Flow<Int>

    @Query("SELECT * FROM transfers WHERE toRecyclerId = :recyclerId ORDER BY initiatedAt DESC")
    fun getByRecycler(recyclerId: String): Flow<List<TransferEntity>>

    @Query("SELECT SUM(COALESCE(weightAtDestination, weightAtSource)) FROM transfers WHERE toRecyclerId = :recyclerId AND status = 'VERIFIED'")
    fun getRecycledWeightSum(recyclerId: String): Flow<Float?>

    @Query("SELECT COUNT(*) FROM transfers WHERE toRecyclerId = :recyclerId AND status = :status")
    fun getCountByRecyclerAndStatus(recyclerId: String, status: String): Flow<Int>

    @Delete
    suspend fun delete(transfer: TransferEntity)
}
