package io.github.mevoc.familybeacon.ui

import android.app.AlertDialog
import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class AuthHelper(private val activity: FragmentActivity) {

    /**
     * true  = fallback 1 (rekommenderat): kräver skärmlås → guidar till inställningar
     * false = fallback 2: read-only dialog (låter dig visa men inte ändra)
     */
    var requireLockForFeature: Boolean = true

    /**
     * Verifiera användaren innan en privilegierad ändring.
     * Kör onSuccess endast om autentisering lyckas.
     */
    fun verifyUser(onSuccess: () -> Unit) {
        val bm = BiometricManager.from(activity)

        val canAuth = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        when (canAuth) {
            BiometricManager.BIOMETRIC_SUCCESS -> showPrompt(onSuccess)
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                // Ofta betyder detta: ingen biometrik, men kan ändå finnas device credential.
                // Vi checkar även om device credential är tillgängligt via canAuthenticate ovan.
                // Om den inte är det → fallback.
                handleNoDeviceLock()
            }
            else -> handleNoDeviceLock()
        }
    }

    private fun showPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User cancelled / error → do nothing
                }

                override fun onAuthenticationFailed() {
                    // Wrong biometric → do nothing
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm change")
            .setSubtitle("Verify using device lock")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun handleNoDeviceLock() {
        if (requireLockForFeature) {
            showEnableLockDialog()
        } else {
            showReadOnlyDialog()
        }
    }

    private fun showEnableLockDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Device lock required")
            .setMessage(
                "To change these settings, the phone must have a screen lock (PIN, pattern, or password). " +
                        "Open settings to enable screen lock?"
            )
            .setCancelable(true)
            .setPositiveButton("Open settings") { _, _ ->
                activity.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    private fun showReadOnlyDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Feature locked")
            .setMessage(
                "This feature can’t be enabled because the device has no screen lock. " +
                        "You can view status, but changes are disabled."
            )
            .setPositiveButton("OK", null)
            .show()
    }
}