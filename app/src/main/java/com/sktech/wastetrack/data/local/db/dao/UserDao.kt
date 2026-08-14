package com.sktech.wastetrack.data.local.db.dao

import androidx.room.*
import com.sktech.wastetrack.data.local.db.entity.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone")
    suspend fun getByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getAnyUser(): UserEntity?

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
