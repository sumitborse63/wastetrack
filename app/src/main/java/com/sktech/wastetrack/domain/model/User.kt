package com.sktech.wastetrack.domain.model

data class User(
    val id: String,
    val name: String,
    val phone: String,
    val role: UserRole,
    val organizationName: String = "",
    val factoryId: String = "",
    val industrialArea: String = "",
    val registrationNumber: String = "",
    val isProfileComplete: Boolean = true,
    val languagePreference: String = "en",
    val createdAt: Long = System.currentTimeMillis()
)
