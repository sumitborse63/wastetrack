package com.sktech.wastetrack.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sktech.wastetrack.domain.model.User
import com.sktech.wastetrack.domain.repository.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val currentUser: User? = null,
    val isLoggingOut: Boolean = false,
    val isSavingProfile: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _state.update { it.copy(currentUser = user) }
        }
    }

    fun updateProfile(name: String, orgName: String, area: String, regNo: String) {
        val current = _state.value.currentUser ?: return
        val updatedUser = current.copy(
            name = name.trim(),
            organizationName = orgName.trim(),
            industrialArea = area.trim(),
            registrationNumber = regNo.trim(),
            isProfileComplete = true
        )

        viewModelScope.launch {
            _state.update { it.copy(isSavingProfile = true) }
            val res = authRepository.saveUserProfile(updatedUser)
            if (res.isSuccess) {
                _state.update { it.copy(currentUser = updatedUser, isSavingProfile = false, message = "Profile updated successfully") }
            } else {
                _state.update { it.copy(isSavingProfile = false, message = "Failed to update profile") }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoggingOut = true) }
            authRepository.logout()
            onComplete()
        }
    }
}
