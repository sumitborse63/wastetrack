package com.sktech.wastetrack.domain.model

data class User(
    val id: String,
    val name: String,
    val phone: String,
    val role: UserRole,
    val factoryId: String,
    val languagePreference: String = "en",
    val createdAt: Long = System.currentTimeMillis()
)
