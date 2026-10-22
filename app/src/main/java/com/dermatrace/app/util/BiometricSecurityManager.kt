package com.dermatrace.app.util

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Biometric security and HIPAA-compliant hardware authentication manager.
 * Supports Fingerprint, Face Unlock, and device fallback credential (PIN/Pattern/Password).
 * Handles backwards compatibility across Android 8 through 14+.
 */
object BiometricSecurityManager {

    private const val PREFS_NAME = "derma_security_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_lock_enabled"

    fun isBiometricLockEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricLockEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isHardwareSupported(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

        // Check if any biometric hardware is available
        val strongAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        val weakAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        val isDeviceSecure = keyguardManager?.isDeviceSecure == true

        return strongAuth == BiometricManager.BIOMETRIC_SUCCESS ||
               weakAuth == BiometricManager.BIOMETRIC_SUCCESS ||
               isDeviceSecure
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "DermaTrace Clinical Authentication",
        subtitle: String = "Verify biometric identity or device PIN to continue",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val biometricManager = BiometricManager.from(activity)

        val hasBiometrics = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS ||
                            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        val hasDeviceLock = keyguardManager?.isDeviceSecure == true

        if (!hasBiometrics && !hasDeviceLock) {
            Toast.makeText(activity, "No screen lock or biometrics set on this device", Toast.LENGTH_LONG).show()
            onError("No security credentials set on device")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    HapticFeedbackHelper.performSuccess(activity)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Don't vibrate loudly on simple user cancel
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        HapticFeedbackHelper.performWarning(activity)
                    }
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    HapticFeedbackHelper.performWarning(activity)
                }
            }
        )

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ supports combining Biometrics with Device Credential
            promptInfoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Android 10 specific compatibility
            if (hasDeviceLock) {
                @Suppress("DEPRECATION")
                promptInfoBuilder.setDeviceCredentialAllowed(true)
            } else {
                promptInfoBuilder.setNegativeButtonText("Cancel")
            }
        } else {
            // Android 9 and lower
            if (hasDeviceLock && !hasBiometrics) {
                @Suppress("DEPRECATION")
                promptInfoBuilder.setDeviceCredentialAllowed(true)
            } else {
                promptInfoBuilder.setNegativeButtonText("Cancel")
            }
        }

        try {
            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            // Fallback with negative button if device credential combination failed on certain OEM ROMs
            try {
                val fallbackPrompt = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButtonText("Cancel")
                    .build()
                biometricPrompt.authenticate(fallbackPrompt)
            } catch (err: Exception) {
                onError(err.message ?: "Authentication failed to initialize")
            }
        }
    }
}
