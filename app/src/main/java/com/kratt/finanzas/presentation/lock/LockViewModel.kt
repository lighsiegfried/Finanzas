package com.kratt.finanzas.presentation.lock

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.kratt.finanzas.R
import com.kratt.finanzas.security.AppLockManager
import com.kratt.finanzas.security.AuthAvailability
import com.kratt.finanzas.security.AuthError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// estados visibles de la pantalla de bloqueo
enum class LockScreenStatus { IDLE, AUTHENTICATING, FAILED, UNAVAILABLE }

data class LockUiState(
    val status: LockScreenStatus = LockScreenStatus.IDLE,
    @StringRes val messageRes: Int? = null,
)

class LockViewModel(
    private val appLockManager: AppLockManager,
    private val checkAvailability: () -> AuthAvailability,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    init {
        refreshAvailability()
    }

    // revisa si el telefono todavia tiene algun metodo de autenticacion
    fun refreshAvailability() {
        val available = checkAvailability() == AuthAvailability.AVAILABLE
        _uiState.value = if (available) {
            if (_uiState.value.status == LockScreenStatus.UNAVAILABLE) LockUiState() else _uiState.value
        } else {
            LockUiState(LockScreenStatus.UNAVAILABLE, R.string.device_lock_unavailable)
        }
    }

    fun onAuthenticationStarted() {
        _uiState.value = LockUiState(LockScreenStatus.AUTHENTICATING)
    }

    fun onAuthenticationSucceeded() {
        _uiState.value = LockUiState()
        appLockManager.unlock()
    }

    // cancelar deja la app bloqueada sin mensaje, fallar muestra el aviso
    fun onAuthenticationError(error: AuthError) {
        _uiState.value = when (error) {
            AuthError.CANCELLED -> LockUiState()
            AuthError.UNAVAILABLE ->
                LockUiState(LockScreenStatus.UNAVAILABLE, R.string.device_lock_unavailable)
            AuthError.FAILED, AuthError.LOCKOUT ->
                LockUiState(LockScreenStatus.FAILED, R.string.auth_failed_message)
        }
    }

    // entrada explicita solo cuando el telefono no tiene ningun bloqueo configurado
    // asi la app nunca deja al usuario afuera para siempre
    fun onContinueWithoutAuth() {
        if (checkAvailability() == AuthAvailability.UNAVAILABLE) {
            appLockManager.unlock()
        } else {
            refreshAvailability()
        }
    }
}
