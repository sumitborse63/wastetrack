package com.sktech.wastetrack.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sktech.wastetrack.domain.model.User
import com.sktech.wastetrack.domain.model.UserRole
import com.sktech.wastetrack.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl @Inject constructor() : IAuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun getCurrentUser(): User? {
        val fbUser = auth.currentUser 
        if (fbUser == null) {
            // Bypass Firebase Auth for now
            return User(
                id = "mock_user_id",
                name = "Mock Supervisor",
                phone = "+919999999999",
                role = UserRole.SUPERVISOR,
                factoryId = "F-001",
                languagePreference = "EN",
                createdAt = System.currentTimeMillis()
            )
        }
        
        return try {
            val doc = firestore.collection("users").document(fbUser.uid).get().await()
            if (doc.exists()) {
                val roleStr = doc.getString("role") ?: UserRole.SUPERVISOR.name
                User(
                    id = fbUser.uid,
                    name = doc.getString("name") ?: fbUser.displayName ?: "User",
                    phone = doc.getString("phone") ?: fbUser.phoneNumber ?: "",
                    role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.SUPERVISOR },
                    factoryId = doc.getString("factoryId") ?: "F-001",
                    languagePreference = doc.getString("languagePreference") ?: "EN",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } else {
                // Fallback to default if document doesn't exist yet
                User(
                    id = fbUser.uid,
                    name = fbUser.displayName ?: "User",
                    phone = fbUser.phoneNumber ?: "",
                    role = UserRole.SUPERVISOR,
                    factoryId = "F-001",
                    languagePreference = "EN",
                    createdAt = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override fun observeAuthState(): Flow<Boolean> = flow {
        // Emit true to bypass login for now
        emit(true)
    }
}
