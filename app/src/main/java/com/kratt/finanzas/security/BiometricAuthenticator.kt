package com.kratt.finanzas.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// disponibilidad del bloqueo del telefono o la biometria
enum class AuthAvailability { AVAILABLE, UNAVAILABLE }

// tipos de error neutros, nunca se exponen codigos crudos al usuario
enum class AuthError { CANCELLED, FAILED, LOCKOUT, UNAVAILABLE }

object BiometricAuthenticator {

    // biometria debil o credencial del telefono, la combinacion mas compatible
    private const val AUTHENTICATORS =
        Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL

    // revisa si hay huella o bloqueo del telefono configurado
    fun availability(context: Context): AuthAvailability =
        when (BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> AuthAvailability.AVAILABLE
            else -> AuthAvailability.UNAVAILABLE
        }

    // muestra el dialogo del sistema, la app nunca ve la credencial ni la huella
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (AuthError) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(mapError(errorCode))
            }
            // los intentos fallidos puntuales los maneja el propio dialogo
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .setConfirmationRequired(false)
            .build()
        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    // traduce los codigos del sistema a tipos neutros y seguros
    private fun mapError(errorCode: Int): AuthError = when (errorCode) {
        BiometricPrompt.ERROR_USER_CANCELED,
        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
        BiometricPrompt.ERROR_CANCELED,
        -> AuthError.CANCELLED

        BiometricPrompt.ERROR_LOCKOUT,
        BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
        -> AuthError.LOCKOUT

        BiometricPrompt.ERROR_HW_NOT_PRESENT,
        BiometricPrompt.ERROR_HW_UNAVAILABLE,
        BiometricPrompt.ERROR_NO_BIOMETRICS,
        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
        -> AuthError.UNAVAILABLE

        else -> AuthError.FAILED
    }
}
