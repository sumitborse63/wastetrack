package com.sktech.wastetrack.data.local.db.dao

import androidx.room.*
import com.sktech.wastetrack.data.local.db.entity.QRHandshakeEntity

@Dao
interface QRHandshakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(handshake: QRHandshakeEntity)

    @Update
    suspend fun update(handshake: QRHandshakeEntity)

    @Query("SELECT * FROM qr_handshakes WHERE transferId = :transferId")
    suspend fun getByTransferId(transferId: String): QRHandshakeEntity?

    @Query("SELECT * FROM qr_handshakes WHERE id = :id")
    suspend fun getById(id: String): QRHandshakeEntity?
}
