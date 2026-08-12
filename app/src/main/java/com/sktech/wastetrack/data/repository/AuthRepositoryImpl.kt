package com.sktech.wastetrack.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.sktech.wastetrack.domain.model.User
import com.sktech.wastetrack.domain.model.UserRole
import com.sktech.wastetrack.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl @Inject constructor() : IAuthRepository {
    private val auth = FirebaseAuth.getInstance()

    override suspend fun getCurrentUser(): User? {
        val fbUser = auth.currentUser ?: return null
        // TODO: Fetch user details from Firestore
        return User(
            id = fbUser.uid,
            name = fbUser.displayName ?: "User",
            phone = fbUser.phoneNumber ?: "",
            role = UserRole.SUPERVISOR, // Default for now
            factoryId = "F-001",
            languagePreference = "EN",
            createdAt = System.currentTimeMillis()
        )
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override fun observeAuthState(): Flow<Boolean> = flow {
        // Emit true if user is logged in
        emit(auth.currentUser != null)
    }
}
