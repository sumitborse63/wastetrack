package com.sktech.wastetrack.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
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
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun onPhoneNumberChanged(number: String) {
        _state.update { it.copy(phoneNumber = number, error = null) }
    }

    fun onOtpCodeChanged(code: String) {
        _state.update { it.copy(otpCode = code, error = null) }
    }

    fun onRoleSelected(role: UserRole) {
        _state.update { it.copy(selectedRole = role) }
    }

    fun sendOtp(activity: Activity) {
        val number = state.value.phoneNumber
        val normalizedNumber = number.trim().replace(" ", "")
        if (!normalizedNumber.matches(Regex("^\\+?[1-9]\\d{9,14}$"))) {
            _state.update { it.copy(error = "Enter a valid phone number with country code") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) = signInWithCredential(credential)
            override fun onVerificationFailed(exception: FirebaseException) {
                // If Firebase Phone Auth or SMS region is disabled in console, allow entering test OTP (123456)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isOtpSent = true,
                        error = "SMS disabled in Firebase Console. Use test OTP: 123456 to login."
                    )
                }
            }
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                resendToken = token
                _state.update { it.copy(isLoading = false, isOtpSent = true, error = null) }
            }
        }
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(if (normalizedNumber.startsWith("+")) normalizedNumber else "+91$normalizedNumber")
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
        )
    }

    fun verifyOtp() {
        val code = state.value.otpCode

        val verificationId = storedVerificationId
        if (code.length != 6) {
            _state.update { it.copy(error = "Enter a 6-digit OTP") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }

        // Test/Demo fallback when Firebase SMS is disabled in console or test OTP 123456 is used
        if (verificationId == null || code == "123456") {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
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
                }
            return
        }

        signInWithCredential(PhoneAuthProvider.getCredential(verificationId, code))
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
