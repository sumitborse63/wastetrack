package com.sktech.wastetrack.domain.model.biometric

import androidx.biometric.BiometricPrompt

/**
 * Represents the biometric availability status on the device.
 */
sealed interface BiometricAvailability {
    data object Available : BiometricAvailability
    data object NoHardware : BiometricAvailability
    data object HardwareUnavailable : BiometricAvailability
    data object NoneEnrolled : BiometricAvailability
    data object SecurityUpdateRequired : BiometricAvailability
    data object Unsupported : BiometricAvailability
    data class Error(val code: Int, val message: String) : BiometricAvailability

    val isAvailable: Boolean
        get() = this is Available
}

/**
 * Result of a biometric authentication attempt.
 */
sealed interface BiometricAuthResult {
    data class Success(val cryptoObject: BiometricPrompt.CryptoObject? = null) : BiometricAuthResult
    data class Error(val errorCode: Int, val errorMessage: String) : BiometricAuthResult
    data object Failed : BiometricAuthResult
    data object Cancelled : BiometricAuthResult
    data class Unavailable(val availability: BiometricAvailability) : BiometricAuthResult
}

/**
 * Configuration options for the Biometric Prompt display.
 */
data class BiometricPromptConfig(
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val negativeButtonText: String = "Cancel",
    val allowDeviceCredential: Boolean = true,
    val requireConfirmation: Boolean = false
)

/**
 * Security level required for the biometric operation.
 */
enum class BiometricSecurityLevel {
    STRONG,           // Class 3 Biometrics (Fingerprint / 3D Face)
    WEAK,             // Class 2 Biometrics
    DEVICE_CREDENTIAL // PIN / Pattern / Password fallback allowed
}
