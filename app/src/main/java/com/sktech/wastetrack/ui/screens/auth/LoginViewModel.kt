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

        _state.update { it.copy(isLoading = true, error = null) }

        val formattedNumber = if (number.startsWith("+")) number else "+91$number"

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    storedVerificationId = verificationId
                    resendToken = token
                    _state.update { it.copy(isLoading = false, isOtpSent = true, error = null) }
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp() {
        val code = state.value.otpCode
        val verificationId = storedVerificationId

        if (code.length < 6 || verificationId == null) {
            _state.update { it.copy(error = "Invalid OTP") }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
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
