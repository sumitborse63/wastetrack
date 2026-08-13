package com.sktech.wastetrack.data.local.db.dao

import androidx.room.*
import com.sktech.wastetrack.data.local.db.entity.CertificateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(certificate: CertificateEntity)

    @Update
    suspend fun update(certificate: CertificateEntity)

    @Query("SELECT * FROM certificates WHERE factoryId = :factoryId ORDER BY generatedAt DESC")
    fun getByFactory(factoryId: String): Flow<List<CertificateEntity>>

    @Query("SELECT * FROM certificates WHERE transferId = :transferId")
    suspend fun getByTransferId(transferId: String): CertificateEntity?

    @Query("SELECT * FROM certificates WHERE id = :id")
    suspend fun getById(id: String): CertificateEntity?

    @Query("SELECT COUNT(*) FROM certificates WHERE factoryId = :factoryId")
    fun getCount(factoryId: String): Flow<Int>

    @Query("SELECT * FROM certificates ORDER BY generatedAt DESC")
    fun getAllCertificates(): Flow<List<CertificateEntity>>
}
