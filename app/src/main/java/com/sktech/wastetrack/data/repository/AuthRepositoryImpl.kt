package com.sktech.wastetrack.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sktech.wastetrack.domain.model.User
import com.sktech.wastetrack.domain.model.UserRole
import com.sktech.wastetrack.domain.repository.IAuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IAuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = context.getSharedPreferences("wastetrack_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LOGGED_IN = "is_logged_in"
        private const val KEY_MOCK_ROLE = "mock_user_role"
    }

    override fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_LOGGED_IN, false)
    }

    override suspend fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply()
    }

    override suspend fun getCurrentUser(): User? {
        val selectedRoleStr = prefs.getString(KEY_MOCK_ROLE, UserRole.SUPERVISOR.name) ?: UserRole.SUPERVISOR.name
        val selectedRole = try { UserRole.valueOf(selectedRoleStr) } catch (e: Exception) { UserRole.SUPERVISOR }
        
        val fbUser = auth.currentUser 
        if (fbUser == null) {
            // Bypass Firebase Auth — return mock user based on selected role
            return if (selectedRole == UserRole.RECYCLER) {
                User(
                    id = "mock_recycler_id",
                    name = "Mumbai Green Recyclers",
                    phone = "+919876543210",
                    role = UserRole.RECYCLER,
                    factoryId = "R-001",
                    languagePreference = "EN",
                    createdAt = System.currentTimeMillis()
                )
            } else {
                User(
                    id = "mock_user_id",
                    name = "Mock Supervisor",
                    phone = "+919999999999",
                    role = UserRole.SUPERVISOR,
                    factoryId = "ambad-midc-pilot-001",
                    languagePreference = "EN",
                    createdAt = System.currentTimeMillis()
                )
            }
        }
        
        return try {
            val doc = firestore.collection("users").document(fbUser.uid).get().await()
            val role = selectedRole
            if (doc.exists()) {
                val name = doc.getString("name") ?: fbUser.displayName ?: "User"
                User(
                    id = fbUser.uid,
                    name = if (role == UserRole.RECYCLER) "Mumbai Green Recyclers" else name,
                    phone = doc.getString("phone") ?: fbUser.phoneNumber ?: "",
                    role = role,
                    factoryId = doc.getString("factoryId") ?: "ambad-midc-pilot-001",
                    languagePreference = doc.getString("languagePreference") ?: "EN",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } else {
                User(
                    id = fbUser.uid,
                    name = if (role == UserRole.RECYCLER) "Mumbai Green Recyclers" else (fbUser.displayName ?: "User"),
                    phone = fbUser.phoneNumber ?: "",
                    role = role,
                    factoryId = "ambad-midc-pilot-001",
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
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .remove(KEY_MOCK_ROLE)
            .apply()
    }

    override fun observeAuthState(): Flow<Boolean> = flow {
        emit(isLoggedIn())
    }

    override suspend fun setMockRole(role: UserRole) {
        prefs.edit().putString(KEY_MOCK_ROLE, role.name).apply()
    }
}

