package com.sktech.wastetrack.util

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.sktech.wastetrack.data.biometric.BiometricAuthManager
import com.sktech.wastetrack.domain.model.biometric.BiometricAuthResult
import com.sktech.wastetrack.domain.model.biometric.BiometricPromptConfig

/**
 * Utility wrapper providing biometric authentication helpers for legacy or static calls.
 */
object SecurityHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        return BiometricAuthManager(context).isBiometricAvailable()
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val manager = BiometricAuthManager(activity.applicationContext)
        val config = BiometricPromptConfig(
            title = title,
            subtitle = subtitle,
            allowDeviceCredential = true
        )

        manager.authenticate(activity, config) { result ->
            when (result) {
                is BiometricAuthResult.Success -> onSuccess()
                is BiometricAuthResult.Error -> onError(result.errorMessage)
                is BiometricAuthResult.Failed -> onError("Authentication failed")
                is BiometricAuthResult.Cancelled -> onError("Authentication cancelled")
                is BiometricAuthResult.Unavailable -> onError("Biometrics not available")
            }
        }
    }
}
