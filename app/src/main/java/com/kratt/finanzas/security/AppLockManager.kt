package com.kratt.finanzas.security

import com.kratt.finanzas.domain.model.LockTimeout
import com.kratt.finanzas.domain.model.SecurityPreferences
import com.kratt.finanzas.domain.repository.SecurityPreferencesRepository
import com.kratt.finanzas.domain.usecase.AutoLockPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// estado de la sesion de bloqueo de toda la app
enum class LockSessionState { UNKNOWN, LOCKED, UNLOCKED }

// guarda en memoria si la app esta bloqueada y aplica el bloqueo automatico
// usa tiempo monotonico inyectado para poder probarlo de forma determinista
class AppLockManager(
    securityPreferencesRepository: SecurityPreferencesRepository,
    private val scope: CoroutineScope,
    private val elapsedRealtime: () -> Long,
) {

    private val _sessionState = MutableStateFlow(LockSessionState.UNKNOWN)
    val sessionState: StateFlow<LockSessionState> = _sessionState.asStateFlow()

    @Volatile
    private var currentPreferences = SecurityPreferences.DEFAULT
    private var backgroundedAtElapsedMillis: Long? = null
    private var pendingLockJob: Job? = null

    init {
        // sigue las preferencias para saber si el bloqueo esta activo
        // al morir el proceso el estado vuelve a empezar bloqueado si aplica
        scope.launch {
            securityPreferencesRepository.preferences.collect { prefs ->
                currentPreferences = prefs
                when {
                    !prefs.appLockEnabled -> _sessionState.value = LockSessionState.UNLOCKED
                    _sessionState.value == LockSessionState.UNKNOWN ->
                        _sessionState.value = LockSessionState.LOCKED
                }
            }
        }
    }

    fun unlock() {
        pendingLockJob?.cancel()
        pendingLockJob = null
        backgroundedAtElapsedMillis = null
        _sessionState.value = LockSessionState.UNLOCKED
    }

    // bloquea de inmediato si el bloqueo esta habilitado
    fun lockNow() {
        if (currentPreferences.appLockEnabled) {
            _sessionState.value = LockSessionState.LOCKED
        }
    }

    // marca la salida a segundo plano y programa el bloqueo al vencer el limite
    fun onAppBackgrounded() {
        if (!currentPreferences.appLockEnabled) return
        if (_sessionState.value != LockSessionState.UNLOCKED) return
        // por sesion no se programa bloqueo, cambiar de app un rato no bloquea
        if (currentPreferences.lockTimeout == LockTimeout.SESSION) return
        backgroundedAtElapsedMillis = elapsedRealtime()
        pendingLockJob?.cancel()
        val timeoutMillis = currentPreferences.lockTimeout.durationMillis
        pendingLockJob = scope.launch {
            delay(timeoutMillis)
            lockNow()
        }
    }

    // al volver revisa con tiempo monotonico si el limite ya se vencio
    fun onAppForegrounded() {
        pendingLockJob?.cancel()
        pendingLockJob = null
        val backgroundedAt = backgroundedAtElapsedMillis ?: return
        backgroundedAtElapsedMillis = null
        if (!currentPreferences.appLockEnabled) return
        if (_sessionState.value != LockSessionState.UNLOCKED) return
        val shouldLock = AutoLockPolicy.shouldLock(
            backgroundedAtElapsedMillis = backgroundedAt,
            nowElapsedMillis = elapsedRealtime(),
            timeout = currentPreferences.lockTimeout,
        )
        if (shouldLock) {
            _sessionState.value = LockSessionState.LOCKED
        }
    }
}
