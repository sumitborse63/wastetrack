package com.sktech.wastetrack.domain.repository

import com.sktech.wastetrack.domain.model.User
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    suspend fun getCurrentUser(): User?
    suspend fun logout()
    fun observeAuthState(): Flow<Boolean>
}
