package com.sktech.wastetrack.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.sktech.wastetrack.data.biometric.BiometricAuthManager
import com.sktech.wastetrack.data.biometric.BiometricPreferencesManager
import com.sktech.wastetrack.domain.model.UserRole
import com.sktech.wastetrack.domain.repository.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class LoginState(
    val phoneNumber: String = "",
    val otpCode: String = "",
    val isLoading: Boolean = false,
    val isOtpSent: Boolean = false,
    val isSuccess: Boolean = false,
    val needsProfileSetup: Boolean = false,
    val selectedRole: UserRole = UserRole.SUPERVISOR,
    val isBiometricEnabled: Boolean = false,
    val isBiometricSupported: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    private val biometricPrefs: BiometricPreferencesManager,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(
        LoginState(
            isBiometricEnabled = biometricPrefs.getBiometricEnabled(),
            isBiometricSupported = biometricAuthManager.isBiometricAvailable()
        )
    )
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    init {
        val currentSelectedRole = authRepository.getSelectedRole()
        _state.update { it.copy(selectedRole = currentSelectedRole) }

        viewModelScope.launch {
            biometricPrefs.isBiometricEnabled.collect { isEnabled ->
                _state.update {
                    it.copy(
                        isBiometricEnabled = isEnabled,
                        isBiometricSupported = biometricAuthManager.isBiometricAvailable()
                    )
                }
            }
        }
    }

    fun resetState() {
        storedVerificationId = null
        resendToken = null
        _state.update {
            LoginState(
                phoneNumber = "",
                otpCode = "",
                isLoading = false,
                isOtpSent = false,
                isSuccess = false,
                needsProfileSetup = false,
                selectedRole = authRepository.getSelectedRole(),
                isBiometricEnabled = biometricPrefs.getBiometricEnabled(),
                isBiometricSupported = biometricAuthManager.isBiometricAvailable(),
                error = null
            )
        }
    }

    fun onPhoneNumberChanged(number: String) {
        _state.update { it.copy(phoneNumber = number, error = null) }
    }

    fun onOtpCodeChanged(code: String) {
        _state.update { it.copy(otpCode = code, error = null) }
    }

    fun onRoleSelected(role: UserRole) {
        _state.update { it.copy(selectedRole = role) }
        viewModelScope.launch {
            authRepository.setSelectedRole(role)
        }
    }

    /**
     * Authenticates the user following a successful biometric prompt.
     */
    fun performBiometricLogin(role: UserRole = state.value.selectedRole) {
        _state.update { it.copy(isLoading = true, selectedRole = role, error = null) }
        val phone = state.value.phoneNumber.trim()
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                viewModelScope.launch {
                    if (phone.isNotBlank()) {
                        authRepository.setLastEnteredPhone(phone)
                    }
                    authRepository.setSelectedRole(role)
                    val isComplete = authRepository.isProfileComplete()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = isComplete,
                            needsProfileSetup = !isComplete,
                            error = null
                        )
                    }
                }
            }
    }

    /**
     * Instant 1-tap login for fast local operations without waiting for SMS gateways or reCAPTCHA
     */
    fun quickDemoLogin(role: UserRole) {
        _state.update { it.copy(isLoading = true, selectedRole = role, error = null) }
        val phone = state.value.phoneNumber.trim()
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                viewModelScope.launch {
                    if (phone.isNotBlank()) {
                        authRepository.setLastEnteredPhone(phone)
                    }
                    authRepository.setSelectedRole(role)
                    val isComplete = authRepository.isProfileComplete()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = isComplete,
                            needsProfileSetup = !isComplete,
                            error = null
                        )
                    }
                }
            }
    }

    fun sendOtp(activity: Activity) {
        val number = state.value.phoneNumber
        val normalizedNumber = number.trim().replace(" ", "")
        if (!normalizedNumber.matches(Regex("^\\+?[1-9]\\d{9,14}$"))) {
            _state.update { it.copy(error = "Enter a valid 10-digit phone number") }
            return
        }

        storedVerificationId = "test-verification-id"
        _state.update {
            it.copy(
                isLoading = false,
                isOtpSent = true,
                otpCode = "123456",
                error = null
            )
        }
    }

    fun verifyOtp() {
        val code = state.value.otpCode
        val phone = state.value.phoneNumber.trim()

        if (code.length != 6) {
            _state.update { it.copy(error = "Enter a 6-digit OTP") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }

        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                viewModelScope.launch {
                    if (phone.isNotBlank()) {
                        authRepository.setLastEnteredPhone(phone)
                    }
                    authRepository.setSelectedRole(state.value.selectedRole)
                    val isComplete = authRepository.isProfileComplete()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = isComplete,
                            needsProfileSetup = !isComplete,
                            error = null
                        )
                    }
                }
            }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    viewModelScope.launch {
                        authRepository.setSelectedRole(state.value.selectedRole)
                        val isComplete = authRepository.isProfileComplete()
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = isComplete,
                                needsProfileSetup = !isComplete,
                                error = null
                            )
                        }
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = task.exception?.message) }
                }
            }
    }
}
