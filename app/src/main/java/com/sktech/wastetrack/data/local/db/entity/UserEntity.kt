package com.sktech.wastetrack.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val role: String = "SUPERVISOR",
    val factoryId: String,
    val languagePreference: String = "en",
    val createdAt: Long = System.currentTimeMillis()
)
