package com.sktech.wastetrack.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sktech.wastetrack.data.local.db.dao.UserDao
import com.sktech.wastetrack.data.local.db.entity.UserEntity
import com.sktech.wastetrack.data.sync.CloudSyncEngine
import com.sktech.wastetrack.domain.model.User
import com.sktech.wastetrack.domain.model.UserRole
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.util.Constants
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao,
    private val cloudSyncEngine: Lazy<CloudSyncEngine>
) : IAuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = context.getSharedPreferences("wastetrack_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "AuthRepository"
        private const val KEY_SELECTED_ROLE = "selected_user_role"
        private const val KEY_CACHED_USER_NAME = "cached_user_name"
        private const val KEY_CACHED_ORG_NAME = "cached_org_name"
        private const val KEY_CACHED_FACTORY_ID = "cached_factory_id"
        private const val KEY_CACHED_AREA = "cached_area"
        private const val KEY_CACHED_REG_NO = "cached_reg_no"
        private const val KEY_IS_PROFILE_COMPLETE = "is_profile_complete"
        private const val KEY_IS_LOGGED_IN = "is_logged_in_pref"
        private const val KEY_LAST_PHONE = "last_entered_phone"

        /**
         * Generates a stable, deterministic user ID based on phone number digits.
         * This prevents random Firebase anonymous UIDs from duplicating user profiles.
         */
        fun generateStableUserId(phone: String, role: UserRole): String {
            val digits = phone.filter { it.isDigit() }
            val last10 = if (digits.length >= 10) digits.takeLast(10) else digits
            val roleSuffix = role.name.lowercase()
            return if (last10.isNotBlank()) {
                "user_${last10}_$roleSuffix"
            } else {
                "user_${roleSuffix}_001"
            }
        }
    }

    override fun isLoggedIn(): Boolean {
        val isExplicitlyLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        return isExplicitlyLoggedIn || auth.currentUser != null
    }

    override suspend fun setLastEnteredPhone(phone: String) {
        val cleanPhone = phone.trim()
        if (cleanPhone.isNotBlank()) {
            prefs.edit().putString(KEY_LAST_PHONE, cleanPhone).apply()
        }
    }

    override fun getLastEnteredPhone(): String {
        return prefs.getString(KEY_LAST_PHONE, "+91 98765 43210") ?: "+91 98765 43210"
    }

    override suspend fun isProfileComplete(): Boolean {
        return isLoggedIn()
    }

    override suspend fun getCurrentUser(): User? {
        if (!isLoggedIn()) {
            return null
        }

        val selectedRole = getSelectedRole()
        val cachedName = prefs.getString(KEY_CACHED_USER_NAME, "") ?: ""
        val cachedOrg = prefs.getString(KEY_CACHED_ORG_NAME, "") ?: ""
        val cachedFactory = prefs.getString(KEY_CACHED_FACTORY_ID, "") ?: ""
        val cachedArea = prefs.getString(KEY_CACHED_AREA, "") ?: ""
        val cachedRegNo = prefs.getString(KEY_CACHED_REG_NO, "") ?: ""
        val phone = getLastEnteredPhone()
        val stableId = generateStableUserId(phone, selectedRole)

        // 1. Try fetching from Firestore by stable ID (phone + role)
        try {
            val doc = withTimeoutOrNull(2500L) {
                firestore.collection("users").document(stableId).get().await()
            }
            if (doc != null && doc.exists()) {
                val orgName = doc.getString("organizationName") ?: ""
                if (orgName.isNotBlank()) {
                    val user = parseUserFromDoc(doc, stableId, phone, selectedRole)
                    userDao.insert(user.toEntity())
                    cacheUserLocally(user)

                    scope.launch {
                        cloudSyncEngine.get().syncAllFromCloud(user.id, user.factoryId, user.role)
                        cloudSyncEngine.get().startRealtimeSync(user.factoryId, user.id)
                    }
                    return user
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching user by stableId from Firestore: ${e.message}")
        }

        // 2. Check local Room DB for this specific role
        val roomEntity = userDao.getById(stableId) 
            ?: userDao.getByPhoneAndRole(phone, selectedRole.name)
            ?: userDao.getByRole(selectedRole.name)
        if (roomEntity != null && roomEntity.organizationName.isNotBlank()) {
            val user = roomEntity.toDomain().copy(
                role = selectedRole,
                factoryId = if (selectedRole == UserRole.RECYCLER && !roomEntity.factoryId.startsWith("REC")) "REC-AMBAD-01" else roomEntity.factoryId
            )
            cacheUserLocally(user)
            scope.launch {
                cloudSyncEngine.get().syncAllFromCloud(user.id, user.factoryId, user.role)
                cloudSyncEngine.get().startRealtimeSync(user.factoryId, user.id)
            }
            return user
        }

        // 3. Fallback to cached SharedPreferences or default active user for this role
        val defaultOrg = when (selectedRole) {
            UserRole.RECYCLER -> "Nashik Green Recyclers Ltd"
            UserRole.DRIVER -> "Apex Industrial Logistics Fleet"
            UserRole.ADMIN -> "Ambad Industrial Estate Authority"
            UserRole.SUPERVISOR -> "Ambad MIDC Manufacturing Unit"
        }
        val defaultFactory = when (selectedRole) {
            UserRole.RECYCLER -> "REC-AMBAD-01"
            UserRole.DRIVER -> "FLEET-MH15-01"
            UserRole.ADMIN -> "FAC-AMBAD-ADMIN"
            UserRole.SUPERVISOR -> "FAC-AMBAD-01"
        }
        val user = User(
            id = stableId,
            name = cachedName.ifBlank { "Sumit Borase" },
            phone = phone,
            role = selectedRole,
            organizationName = if (cachedOrg.contains("Manufacturing") && selectedRole == UserRole.RECYCLER) defaultOrg else cachedOrg.ifBlank { defaultOrg },
            factoryId = if (selectedRole == UserRole.RECYCLER && !cachedFactory.startsWith("REC")) defaultFactory else cachedFactory.ifBlank { defaultFactory },
            industrialArea = cachedArea.ifBlank { "Ambad MIDC, Nashik" },
            registrationNumber = cachedRegNo.ifBlank { if (selectedRole == UserRole.RECYCLER) "MPCB/REC/2024/008" else "MH-NSK-2024-MSME" },
            isProfileComplete = true,
            languagePreference = "EN",
            createdAt = System.currentTimeMillis()
        )
        userDao.insert(user.toEntity())
        cacheUserLocally(user)
        scope.launch {
            cloudSyncEngine.get().syncAllFromCloud(user.id, user.factoryId, user.role)
            cloudSyncEngine.get().startRealtimeSync(user.factoryId, user.id)
        }
        return user
    }

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            val stableId = generateStableUserId(user.phone, user.role)
            val completeUser = user.copy(id = stableId, isProfileComplete = true)

            // 1. Cache to Room DB immediately
            userDao.insert(completeUser.toEntity())
            cacheUserLocally(completeUser)

            // 2. Persist to Firestore under stableId
            val userMap = hashMapOf(
                "id" to completeUser.id,
                "name" to completeUser.name,
                "phone" to completeUser.phone,
                "role" to completeUser.role.name,
                "organizationName" to completeUser.organizationName,
                "factoryId" to completeUser.factoryId,
                "industrialArea" to completeUser.industrialArea,
                "registrationNumber" to completeUser.registrationNumber,
                "isProfileComplete" to true,
                "languagePreference" to completeUser.languagePreference,
                "createdAt" to completeUser.createdAt
            )
            firestore.collection("users").document(stableId).set(userMap).await()

            // Also mirror to auth.currentUser.uid if present
            auth.currentUser?.let { fbUser ->
                try {
                    firestore.collection("users").document(fbUser.uid).set(userMap).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed mirroring user to auth.uid: ${e.message}")
                }
            }

            scope.launch {
                cloudSyncEngine.get().syncAllFromCloud(completeUser.id, completeUser.factoryId, completeUser.role)
                cloudSyncEngine.get().startRealtimeSync(completeUser.factoryId, completeUser.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveUserProfile failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        try {
            cloudSyncEngine.get().stopRealtimeSync()
        } catch (e: Exception) {
            Log.w(TAG, "Failed stopping realtime sync: ${e.message}")
        }
        auth.signOut()
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putBoolean(KEY_IS_PROFILE_COMPLETE, false)
            .putString(KEY_CACHED_USER_NAME, "")
            .putString(KEY_CACHED_ORG_NAME, "")
            .putString(KEY_CACHED_FACTORY_ID, "")
            .putString(KEY_CACHED_AREA, "")
            .putString(KEY_CACHED_REG_NO, "")
            .apply()
        userDao.deleteAll()
    }

    override fun observeAuthState(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null || prefs.getBoolean(KEY_IS_LOGGED_IN, false))
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun setSelectedRole(role: UserRole) {
        prefs.edit()
            .putString(KEY_SELECTED_ROLE, role.name)
            .apply()
        val phone = getLastEnteredPhone()
        val stableId = generateStableUserId(phone, role)
        val existing = userDao.getById(stableId) ?: userDao.getByPhoneAndRole(phone, role.name) ?: userDao.getByRole(role.name)
        if (existing != null) {
            userDao.update(existing.copy(role = role.name))
        }
    }

    override fun getSelectedRole(): UserRole {
        val roleStr = prefs.getString(KEY_SELECTED_ROLE, UserRole.SUPERVISOR.name)
        return try {
            UserRole.valueOf(roleStr ?: UserRole.SUPERVISOR.name)
        } catch (e: Exception) {
            UserRole.SUPERVISOR
        }
    }

    private fun cacheUserLocally(user: User) {
        prefs.edit()
            .putString(KEY_CACHED_USER_NAME, user.name)
            .putString(KEY_CACHED_ORG_NAME, user.organizationName)
            .putString(KEY_CACHED_FACTORY_ID, user.factoryId)
            .putString(KEY_CACHED_AREA, user.industrialArea)
            .putString(KEY_CACHED_REG_NO, user.registrationNumber)
            .putString(KEY_SELECTED_ROLE, user.role.name)
            .putString(KEY_LAST_PHONE, user.phone)
            .putBoolean(KEY_IS_PROFILE_COMPLETE, true)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    private fun parseUserFromDoc(doc: DocumentSnapshot, fallbackId: String, fallbackPhone: String, selectedRole: UserRole): User {
        val roleStr = doc.getString("role") ?: selectedRole.name
        val role = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(selectedRole)
        val defaultFactory = when (role) {
            UserRole.RECYCLER -> "REC-AMBAD-01"
            UserRole.DRIVER -> "FLEET-MH15-01"
            UserRole.ADMIN -> "FAC-AMBAD-ADMIN"
            UserRole.SUPERVISOR -> "FAC-AMBAD-01"
        }
        return User(
            id = doc.getString("id") ?: fallbackId,
            name = doc.getString("name") ?: "Sumit Borase",
            phone = doc.getString("phone") ?: fallbackPhone,
            role = role,
            organizationName = doc.getString("organizationName") ?: "",
            factoryId = doc.getString("factoryId") ?: defaultFactory,
            industrialArea = doc.getString("industrialArea") ?: "Ambad MIDC, Nashik",
            registrationNumber = doc.getString("registrationNumber") ?: "MH-NSK-2024-MSME",
            isProfileComplete = doc.getBoolean("isProfileComplete") ?: true,
            languagePreference = doc.getString("languagePreference") ?: "EN",
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun UserEntity.toDomain(): User {
        val parsedRole = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.SUPERVISOR)
        return User(
            id = id,
            name = name,
            phone = phone,
            role = parsedRole,
            organizationName = organizationName,
            factoryId = factoryId,
            industrialArea = industrialArea,
            registrationNumber = registrationNumber,
            isProfileComplete = isProfileComplete,
            languagePreference = languagePreference,
            createdAt = createdAt
        )
    }

    private fun User.toEntity(): UserEntity {
        return UserEntity(
            id = id,
            name = name,
            phone = phone,
            role = role.name,
            organizationName = organizationName,
            factoryId = factoryId,
            industrialArea = industrialArea,
            registrationNumber = registrationNumber,
            isProfileComplete = isProfileComplete,
            languagePreference = languagePreference,
            createdAt = createdAt
        )
    }
}
