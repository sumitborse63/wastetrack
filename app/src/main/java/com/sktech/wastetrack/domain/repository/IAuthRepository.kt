package com.sktech.wastetrack.domain.repository

import com.sktech.wastetrack.domain.model.User
import com.sktech.wastetrack.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    suspend fun getCurrentUser(): User?
    suspend fun logout()
    fun observeAuthState(): Flow<Boolean>
    suspend fun setMockRole(role: UserRole)
    fun isLoggedIn(): Boolean
    suspend fun setLoggedIn(loggedIn: Boolean)
}

