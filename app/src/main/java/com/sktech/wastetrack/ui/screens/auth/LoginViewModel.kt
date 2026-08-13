package com.sktech.wastetrack.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
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
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

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

    fun sendOtp(activity: Activity) {
        val number = state.value.phoneNumber
        if (number.length < 10) {
            _state.update { it.copy(error = "Invalid phone number") }
            return
        }

        // Bypass Firebase OTP for now
        storedVerificationId = "mock"
        _state.update { it.copy(isLoading = false, isOtpSent = true, error = null) }
    }

    fun verifyOtp() {
        val code = state.value.otpCode

        if (code.length < 6) {
            _state.update { it.copy(error = "Invalid OTP") }
            return
        }

        // Bypass Firebase OTP for now
        _state.update { it.copy(isLoading = false, isSuccess = true, error = null) }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _state.update { it.copy(isLoading = false, isSuccess = true, error = null) }
                } else {
                    _state.update { it.copy(isLoading = false, error = task.exception?.message) }
                }
            }
    }
}
