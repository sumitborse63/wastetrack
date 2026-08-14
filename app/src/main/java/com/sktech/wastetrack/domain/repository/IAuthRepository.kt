package com.sktech.wastetrack.domain.repository

import com.sktech.wastetrack.domain.model.User
import com.sktech.wastetrack.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    suspend fun getCurrentUser(): User?
    suspend fun saveUserProfile(user: User): Result<Unit>
    suspend fun isProfileComplete(): Boolean
    suspend fun logout()
    fun observeAuthState(): Flow<Boolean>
    suspend fun setSelectedRole(role: UserRole)
    fun getSelectedRole(): UserRole
    fun isLoggedIn(): Boolean
}
