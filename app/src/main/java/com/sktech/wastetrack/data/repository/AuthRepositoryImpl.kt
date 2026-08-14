package com.sktech.wastetrack.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sktech.wastetrack.data.local.db.dao.UserDao
import com.sktech.wastetrack.data.local.db.entity.UserEntity
import com.sktech.wastetrack.domain.model.User
import com.sktech.wastetrack.domain.model.UserRole
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao
) : IAuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = context.getSharedPreferences("wastetrack_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SELECTED_ROLE = "selected_user_role"
        private const val KEY_CACHED_USER_NAME = "cached_user_name"
        private const val KEY_CACHED_ORG_NAME = "cached_org_name"
        private const val KEY_CACHED_FACTORY_ID = "cached_factory_id"
        private const val KEY_CACHED_AREA = "cached_area"
        private const val KEY_CACHED_REG_NO = "cached_reg_no"
        private const val KEY_IS_PROFILE_COMPLETE = "is_profile_complete"
        private const val KEY_IS_LOGGED_IN = "is_logged_in_pref"
    }

    override fun isLoggedIn(): Boolean {
        return auth.currentUser != null || prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    override suspend fun isProfileComplete(): Boolean {
        val user = getCurrentUser()
        return user != null && user.isProfileComplete && user.organizationName.isNotBlank()
    }

    override suspend fun getCurrentUser(): User? {
        val selectedRoleStr = prefs.getString(KEY_SELECTED_ROLE, UserRole.SUPERVISOR.name) ?: UserRole.SUPERVISOR.name
        val selectedRole = try { UserRole.valueOf(selectedRoleStr) } catch (e: Exception) { UserRole.SUPERVISOR }
        
        val fbUser = auth.currentUser
        
        if (fbUser == null) {
            // Local / Offline fallback user using cached SharedPreferences & Room
            val cachedName = prefs.getString(KEY_CACHED_USER_NAME, "") ?: ""
            val cachedOrg = prefs.getString(KEY_CACHED_ORG_NAME, "") ?: ""
            val cachedFactory = prefs.getString(KEY_CACHED_FACTORY_ID, "") ?: ""
            val cachedArea = prefs.getString(KEY_CACHED_AREA, "") ?: ""
            val cachedRegNo = prefs.getString(KEY_CACHED_REG_NO, "") ?: ""
            val isComplete = prefs.getBoolean(KEY_IS_PROFILE_COMPLETE, false)

            if (cachedName.isBlank()) {
                return null
            }

            return User(
                id = "local-user-001",
                name = cachedName,
                phone = "+91 94035 80730",
                role = selectedRole,
                organizationName = cachedOrg,
                factoryId = cachedFactory,
                industrialArea = cachedArea,
                registrationNumber = cachedRegNo,
                isProfileComplete = isComplete,
                languagePreference = "EN",
                createdAt = System.currentTimeMillis()
            )
        }
        
        return try {
            val doc = withTimeoutOrNull(3000L) {
                firestore.collection("users").document(fbUser.uid).get().await()
            }
            
            if (doc != null && doc.exists()) {
                val name = doc.getString("name") ?: ""
                val roleStr = doc.getString("role") ?: selectedRole.name
                val role = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(selectedRole)
                val orgName = doc.getString("organizationName") ?: ""
                val factoryId = doc.getString("factoryId") ?: ""
                val area = doc.getString("industrialArea") ?: ""
                val regNo = doc.getString("registrationNumber") ?: ""
                val isComplete = doc.getBoolean("isProfileComplete") ?: (orgName.isNotBlank())
                val lang = doc.getString("languagePreference") ?: "EN"
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                val user = User(
                    id = fbUser.uid,
                    name = name,
                    phone = doc.getString("phone") ?: fbUser.phoneNumber ?: "",
                    role = role,
                    organizationName = orgName,
                    factoryId = factoryId,
                    industrialArea = area,
                    registrationNumber = regNo,
                    isProfileComplete = isComplete,
                    languagePreference = lang,
                    createdAt = createdAt
                )

                // Update local caches
                userDao.insert(
                    UserEntity(
                        id = user.id,
                        name = user.name,
                        phone = user.phone,
                        role = user.role.name,
                        organizationName = user.organizationName,
                        factoryId = user.factoryId,
                        industrialArea = user.industrialArea,
                        registrationNumber = user.registrationNumber,
                        isProfileComplete = user.isProfileComplete,
                        languagePreference = user.languagePreference,
                        createdAt = user.createdAt
                    )
                )

                prefs.edit()
                    .putString(KEY_CACHED_USER_NAME, user.name)
                    .putString(KEY_CACHED_ORG_NAME, user.organizationName)
                    .putString(KEY_CACHED_FACTORY_ID, user.factoryId)
                    .putString(KEY_CACHED_AREA, user.industrialArea)
                    .putString(KEY_CACHED_REG_NO, user.registrationNumber)
                    .putString(KEY_SELECTED_ROLE, user.role.name)
                    .putBoolean(KEY_IS_PROFILE_COMPLETE, user.isProfileComplete)
                    .putBoolean(KEY_IS_LOGGED_IN, true)
                    .apply()

                user
            } else {
                // Check local Room DB first
                val cachedEntity = userDao.getById(fbUser.uid)
                if (cachedEntity != null && cachedEntity.organizationName.isNotBlank()) {
                    User(
                        id = cachedEntity.id,
                        name = cachedEntity.name,
                        phone = cachedEntity.phone,
                        role = runCatching { UserRole.valueOf(cachedEntity.role) }.getOrDefault(selectedRole),
                        organizationName = cachedEntity.organizationName,
                        factoryId = cachedEntity.factoryId,
                        industrialArea = cachedEntity.industrialArea,
                        registrationNumber = cachedEntity.registrationNumber,
                        isProfileComplete = cachedEntity.isProfileComplete,
                        languagePreference = cachedEntity.languagePreference,
                        createdAt = cachedEntity.createdAt
                    )
                } else {
                    // New unconfigured user
                    User(
                        id = fbUser.uid,
                        name = fbUser.displayName.orEmpty(),
                        phone = fbUser.phoneNumber ?: "",
                        role = selectedRole,
                        organizationName = "",
                        factoryId = "",
                        industrialArea = "",
                        registrationNumber = "",
                        isProfileComplete = false,
                        languagePreference = "EN",
                        createdAt = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            // Offline fallback from Room DB
            val cachedEntity = userDao.getById(fbUser.uid)
            if (cachedEntity != null) {
                User(
                    id = cachedEntity.id,
                    name = cachedEntity.name,
                    phone = cachedEntity.phone,
                    role = runCatching { UserRole.valueOf(cachedEntity.role) }.getOrDefault(selectedRole),
                    organizationName = cachedEntity.organizationName,
                    factoryId = cachedEntity.factoryId,
                    industrialArea = cachedEntity.industrialArea,
                    registrationNumber = cachedEntity.registrationNumber,
                    isProfileComplete = cachedEntity.isProfileComplete,
                    languagePreference = cachedEntity.languagePreference,
                    createdAt = cachedEntity.createdAt
                )
            } else {
                User(
                    id = fbUser.uid,
                    name = prefs.getString(KEY_CACHED_USER_NAME, "") ?: "",
                    phone = fbUser.phoneNumber ?: "",
                    role = selectedRole,
                    organizationName = prefs.getString(KEY_CACHED_ORG_NAME, "") ?: "",
                    factoryId = prefs.getString(KEY_CACHED_FACTORY_ID, "") ?: "",
                    industrialArea = prefs.getString(KEY_CACHED_AREA, "") ?: "",
                    registrationNumber = prefs.getString(KEY_CACHED_REG_NO, "") ?: "",
                    isProfileComplete = prefs.getBoolean(KEY_IS_PROFILE_COMPLETE, false),
                    languagePreference = "EN",
                    createdAt = System.currentTimeMillis()
                )
            }
        }
    }

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            val userEntity = UserEntity(
                id = user.id,
                name = user.name,
                phone = user.phone,
                role = user.role.name,
                organizationName = user.organizationName,
                factoryId = user.factoryId,
                industrialArea = user.industrialArea,
                registrationNumber = user.registrationNumber,
                isProfileComplete = true,
                languagePreference = user.languagePreference,
                createdAt = user.createdAt
            )

            // 1. Cache to Room DB immediately
            userDao.insert(userEntity)

            // 2. Cache to SharedPreferences
            prefs.edit()
                .putString(KEY_CACHED_USER_NAME, user.name)
                .putString(KEY_CACHED_ORG_NAME, user.organizationName)
                .putString(KEY_CACHED_FACTORY_ID, user.factoryId)
                .putString(KEY_CACHED_AREA, user.industrialArea)
                .putString(KEY_CACHED_REG_NO, user.registrationNumber)
                .putString(KEY_SELECTED_ROLE, user.role.name)
                .putBoolean(KEY_IS_PROFILE_COMPLETE, true)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply()

            // 3. Sync to Firestore
            withTimeoutOrNull(4000L) {
                firestore.collection("users").document(user.id).set(
                    mapOf(
                        "id" to user.id,
                        "name" to user.name,
                        "phone" to user.phone,
                        "role" to user.role.name,
                        "organizationName" to user.organizationName,
                        "factoryId" to user.factoryId,
                        "industrialArea" to user.industrialArea,
                        "registrationNumber" to user.registrationNumber,
                        "isProfileComplete" to true,
                        "languagePreference" to user.languagePreference,
                        "createdAt" to user.createdAt
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        auth.signOut()
        userDao.deleteAll()
        prefs.edit().clear().apply()
    }

    override fun observeAuthState(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null || prefs.getBoolean(KEY_IS_LOGGED_IN, false))
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun setSelectedRole(role: UserRole) {
        prefs.edit().putString(KEY_SELECTED_ROLE, role.name).apply()
    }
}
