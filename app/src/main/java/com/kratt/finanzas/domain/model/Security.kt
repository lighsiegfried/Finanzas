package com.kratt.finanzas.domain.model

// opciones de bloqueo automatico
// por sesion no vence por tiempo, solo la muerte del proceso obliga a autenticar
enum class LockTimeout(val durationMillis: Long) {
    SESSION(Long.MAX_VALUE),
    TEN_MINUTES(600_000L),
}

// preferencias de seguridad no sensibles que se guardan en datastore
data class SecurityPreferences(
    val appLockEnabled: Boolean,
    val lockTimeout: LockTimeout,
) {
    companion object {
        val DEFAULT = SecurityPreferences(
            appLockEnabled = false,
            lockTimeout = LockTimeout.SESSION,
        )
    }
}
