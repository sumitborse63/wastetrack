package com.sktech.wastetrack.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
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
        private const val KEY_SELECTED_ROLE = "selected_user_role"
        private const val KEY_CACHED_USER_NAME = "cached_user_name"
        private const val KEY_CACHED_ORG_NAME = "cached_org_name"
        private const val KEY_CACHED_FACTORY_ID = "cached_factory_id"
        private const val KEY_CACHED_AREA = "cached_area"
        private const val KEY_CACHED_REG_NO = "cached_reg_no"
        private const val KEY_IS_PROFILE_COMPLETE = "is_profile_complete"
        private const val KEY_IS_LOGGED_IN = "is_logged_in_pref"
    }

    private data class RoleDefaults(
        val org: String,
        val factoryId: String,
        val area: String,
        val regNo: String
    )

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
        
        val cachedName = prefs.getString(KEY_CACHED_USER_NAME, "") ?: ""
        val cachedOrg = prefs.getString(KEY_CACHED_ORG_NAME, "") ?: ""
        val cachedFactory = prefs.getString(KEY_CACHED_FACTORY_ID, "") ?: ""
        val cachedArea = prefs.getString(KEY_CACHED_AREA, "") ?: ""
        val cachedRegNo = prefs.getString(KEY_CACHED_REG_NO, "") ?: ""
        val isCompletePref = prefs.getBoolean(KEY_IS_PROFILE_COMPLETE, false)

        val fbUser = auth.currentUser
        val userId = fbUser?.uid ?: "local-user-001"
        val userPhone = fbUser?.phoneNumber?.ifBlank { null } ?: "+91 94035 80730"

        // 1. Try fetching from Firestore if user is authenticated with Firebase
        if (fbUser != null) {
            try {
                val doc = withTimeoutOrNull(2500L) {
                    firestore.collection("users").document(fbUser.uid).get().await()
                }
                if (doc != null && doc.exists()) {
                    val name = doc.getString("name")?.ifBlank { null } ?: cachedName.ifBlank { fbUser.displayName.orEmpty() }
                    val roleStr = doc.getString("role") ?: selectedRole.name
                    val role = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(selectedRole)
                    val orgName = doc.getString("organizationName")?.ifBlank { null } ?: cachedOrg
                    val factoryId = doc.getString("factoryId")?.ifBlank { null } ?: cachedFactory
                    val area = doc.getString("industrialArea")?.ifBlank { null } ?: cachedArea
                    val regNo = doc.getString("registrationNumber")?.ifBlank { null } ?: cachedRegNo
                    val isComplete = doc.getBoolean("isProfileComplete") ?: (orgName.isNotBlank())
                    val lang = doc.getString("languagePreference") ?: "EN"
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                    val user = User(
                        id = fbUser.uid,
                        name = name.ifBlank { "Sumit Borase" },
                        phone = doc.getString("phone") ?: userPhone,
                        role = role,
                        organizationName = orgName,
                        factoryId = factoryId,
                        industrialArea = area,
                        registrationNumber = regNo,
                        isProfileComplete = isComplete && orgName.isNotBlank(),
                        languagePreference = lang,
                        createdAt = createdAt
                    )

                    // Cache locally in Room & SharedPreferences
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

                    // Rehydrate all cloud records in background
                    scope.launch {
                        cloudSyncEngine.get().syncAllFromCloud(user.id, user.factoryId, user.role)
                        cloudSyncEngine.get().startRealtimeSync(user.factoryId, user.id)
                    }

                    return user
                }
            } catch (e: Exception) {
                // Firestore offline or error, continue to local Room/SharedPreferences resolution
            }
        }

        // 2. Check local Room DB
        val roomEntity = if (fbUser != null) {
            userDao.getById(fbUser.uid) ?: userDao.getByPhone(userPhone) ?: userDao.getAnyUser()
        } else {
            userDao.getAnyUser()
        }

        if (roomEntity != null && roomEntity.organizationName.isNotBlank()) {
            val user = User(
                id = userId,
                name = roomEntity.name.ifBlank { cachedName.ifBlank { "Sumit Borase" } },
                phone = userPhone,
                role = runCatching { UserRole.valueOf(roomEntity.role) }.getOrDefault(selectedRole),
                organizationName = roomEntity.organizationName,
                factoryId = roomEntity.factoryId,
                industrialArea = roomEntity.industrialArea,
                registrationNumber = roomEntity.registrationNumber,
                isProfileComplete = true,
                languagePreference = roomEntity.languagePreference,
                createdAt = roomEntity.createdAt
            )
            scope.launch {
                cloudSyncEngine.get().syncAllFromCloud(user.id, user.factoryId, user.role)
                cloudSyncEngine.get().startRealtimeSync(user.factoryId, user.id)
            }
            return user
        }

        // 3. Fallback to cached SharedPreferences
        if (cachedOrg.isNotBlank() || isCompletePref) {
            val user = User(
                id = userId,
                name = cachedName.ifBlank { "Sumit Borase" },
                phone = userPhone,
                role = selectedRole,
                organizationName = cachedOrg.ifBlank { "Ambad MIDC Manufacturing Unit" },
                factoryId = cachedFactory.ifBlank { "FAC-AMBAD-01" },
                industrialArea = cachedArea.ifBlank { "Ambad MIDC, Nashik" },
                registrationNumber = cachedRegNo.ifBlank { "MH-NSK-2024-MSME" },
                isProfileComplete = true,
                languagePreference = "EN",
                createdAt = System.currentTimeMillis()
            )
            scope.launch {
                cloudSyncEngine.get().syncAllFromCloud(user.id, user.factoryId, user.role)
                cloudSyncEngine.get().startRealtimeSync(user.factoryId, user.id)
            }
            return user
        }

        // If not logged in at all and no prefs
        if (!isLoggedIn()) {
            return null
        }

        // Return a default ready user so user is never locked in a blank state
        val defaultUser = User(
            id = userId,
            name = "Sumit Borase",
            phone = userPhone,
            role = selectedRole,
            organizationName = if (selectedRole == UserRole.RECYCLER) "Nashik Green Recyclers Ltd" else "Ambad MIDC Manufacturing Unit",
            factoryId = if (selectedRole == UserRole.RECYCLER) "REC-NSK-01" else "FAC-AMBAD-01",
            industrialArea = "Ambad MIDC, Nashik",
            registrationNumber = "MH-NSK-2024-MSME",
            isProfileComplete = true,
            languagePreference = "EN",
            createdAt = System.currentTimeMillis()
        )
        scope.launch {
            cloudSyncEngine.get().syncAllFromCloud(defaultUser.id, defaultUser.factoryId, defaultUser.role)
            cloudSyncEngine.get().startRealtimeSync(defaultUser.factoryId, defaultUser.id)
        }
        return defaultUser
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
                    ),
                    SetOptions.merge()
                ).await()
            }

            scope.launch {
                cloudSyncEngine.get().syncAllFromCloud(user.id, user.factoryId, user.role)
                cloudSyncEngine.get().startRealtimeSync(user.factoryId, user.id)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        cloudSyncEngine.get().stopRealtimeSync()
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
        val currentOrg = prefs.getString(KEY_CACHED_ORG_NAME, "") ?: ""
        val editor = prefs.edit().putString(KEY_SELECTED_ROLE, role.name)
        
        // If no custom profile exists yet, set sensible defaults so the user is never stuck
        if (currentOrg.isBlank()) {
            val defaultName = "Sumit Borase"
            val defaults = when (role) {
                UserRole.SUPERVISOR -> RoleDefaults("Ambad MIDC Manufacturing Unit", "FAC-AMBAD-01", "Ambad MIDC, Nashik", "MH-NSK-2024-MSME")
                UserRole.RECYCLER -> RoleDefaults("Nashik Green Recyclers Ltd", "REC-NSK-01", "Sinnar MIDC, Nashik", "MPCB-REC-2024-089")
                UserRole.DRIVER -> RoleDefaults("AstraNyx Logistics Fleet", "FLT-NSK-01", "Ambad MIDC, Nashik", "MH-15-TR-2024")
                UserRole.ADMIN -> RoleDefaults("Maharashtra Pollution Control Board", "ADM-MPCB-01", "Nashik Regional Office", "MPCB-ADMIN-001")
            }
            editor.putString(KEY_CACHED_USER_NAME, defaultName)
                .putString(KEY_CACHED_ORG_NAME, defaults.org)
                .putString(KEY_CACHED_FACTORY_ID, defaults.factoryId)
                .putString(KEY_CACHED_AREA, defaults.area)
                .putString(KEY_CACHED_REG_NO, defaults.regNo)
                .putBoolean(KEY_IS_PROFILE_COMPLETE, true)
                .putBoolean(KEY_IS_LOGGED_IN, true)
        }
        editor.apply()
    }

    override fun getSelectedRole(): UserRole {
        val roleStr = prefs.getString(KEY_SELECTED_ROLE, UserRole.SUPERVISOR.name) ?: UserRole.SUPERVISOR.name
        return try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.SUPERVISOR }
    }
}
