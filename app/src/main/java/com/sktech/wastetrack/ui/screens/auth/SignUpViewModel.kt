package com.sktech.wastetrack.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sktech.wastetrack.domain.model.User
import com.sktech.wastetrack.domain.model.UserRole
import com.sktech.wastetrack.domain.repository.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SignUpState(
    val name: String = "",
    val role: UserRole = UserRole.SUPERVISOR,
    val organizationName: String = "",
    val factoryId: String = "",
    val industrialArea: String = "",
    val registrationNumber: String = "",
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(SignUpState())
    val state: StateFlow<SignUpState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _state.update {
                    it.copy(
                        name = user.name,
                        role = user.role,
                        organizationName = user.organizationName,
                        factoryId = user.factoryId.ifBlank { "FAC-${UUID.randomUUID().toString().take(6).uppercase()}" },
                        industrialArea = user.industrialArea,
                        registrationNumber = user.registrationNumber
                    )
                }
            } else {
                _state.update {
                    it.copy(factoryId = "FAC-${UUID.randomUUID().toString().take(6).uppercase()}")
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name, error = null) }
    }

    fun onRoleSelected(role: UserRole) {
        _state.update {
            it.copy(
                role = role,
                factoryId = if (role == UserRole.RECYCLER) "REC-${UUID.randomUUID().toString().take(6).uppercase()}" else "FAC-${UUID.randomUUID().toString().take(6).uppercase()}"
            )
        }
    }

    fun onOrganizationNameChanged(orgName: String) {
        _state.update { it.copy(organizationName = orgName, error = null) }
    }

    fun onFactoryIdChanged(factoryId: String) {
        _state.update { it.copy(factoryId = factoryId, error = null) }
    }

    fun onIndustrialAreaChanged(area: String) {
        _state.update { it.copy(industrialArea = area, error = null) }
    }

    fun onRegistrationNumberChanged(regNo: String) {
        _state.update { it.copy(registrationNumber = regNo, error = null) }
    }

    fun completeProfile() {
        val s = _state.value
        if (s.name.trim().isBlank()) {
            _state.update { it.copy(error = "Please enter your full name") }
            return
        }
        if (s.organizationName.trim().isBlank()) {
            _state.update {
                it.copy(error = if (s.role == UserRole.RECYCLER) "Please enter your recycling business name" else "Please enter your factory / plant name")
            }
            return
        }
        if (s.industrialArea.trim().isBlank()) {
            _state.update { it.copy(error = "Please enter your industrial area or city") }
            return
        }

        val uid = auth.currentUser?.uid ?: "user-${UUID.randomUUID().toString().take(8)}"
        val phone = auth.currentUser?.phoneNumber ?: "+91 94035 80730"

        val user = User(
            id = uid,
            name = s.name.trim(),
            phone = phone,
            role = s.role,
            organizationName = s.organizationName.trim(),
            factoryId = s.factoryId.trim().ifBlank { if (s.role == UserRole.RECYCLER) "REC-${UUID.randomUUID().toString().take(6).uppercase()}" else "FAC-${UUID.randomUUID().toString().take(6).uppercase()}" },
            industrialArea = s.industrialArea.trim(),
            registrationNumber = s.registrationNumber.trim(),
            isProfileComplete = true,
            languagePreference = "EN",
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val result = authRepository.saveUserProfile(user)
            if (result.isSuccess) {
                _state.update { it.copy(isSaving = false, isSuccess = true) }
            } else {
                _state.update { it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Failed to save profile") }
            }
        }
    }
}
