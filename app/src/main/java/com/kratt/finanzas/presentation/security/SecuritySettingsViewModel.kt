package com.kratt.finanzas.presentation.security

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.domain.model.LockTimeout
import com.kratt.finanzas.domain.repository.SecurityPreferencesRepository
import com.kratt.finanzas.security.AuthAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecuritySettingsUiState(
    val isLoading: Boolean = true,
    val lockEnabled: Boolean = false,
    val timeout: LockTimeout = LockTimeout.SESSION,
    val authAvailable: Boolean = false,
    @StringRes val enableErrorRes: Int? = null,
)

class SecuritySettingsViewModel(
    private val securityPreferencesRepository: SecurityPreferencesRepository,
    private val checkAvailability: () -> AuthAvailability,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecuritySettingsUiState())
    val uiState: StateFlow<SecuritySettingsUiState> = _uiState.asStateFlow()

    init {
        refreshAvailability()
        // refleja las preferencias guardadas en la pantalla
        viewModelScope.launch {
            securityPreferencesRepository.preferences.collect { prefs ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        lockEnabled = prefs.appLockEnabled,
                        timeout = prefs.lockTimeout,
                    )
                }
            }
        }
    }

    // revisa si el telefono tiene huella o bloqueo configurado
    fun refreshAvailability() {
        _uiState.update {
            it.copy(authAvailable = checkAvailability() == AuthAvailability.AVAILABLE)
        }
    }

    // se llama solo despues de que el sistema confirmo la identidad
    fun onLockEnableConfirmed() {
        viewModelScope.launch {
            securityPreferencesRepository.setAppLockEnabled(true)
        }
    }

    fun onLockDisabled() {
        viewModelScope.launch {
            securityPreferencesRepository.setAppLockEnabled(false)
        }
    }

    fun onTimeoutSelected(timeout: LockTimeout) {
        viewModelScope.launch {
            securityPreferencesRepository.setLockTimeout(timeout)
        }
    }

    fun onEnableAuthFailed() {
        _uiState.update { it.copy(enableErrorRes = R.string.auth_failed_message) }
    }

    fun onEnableErrorShown() {
        _uiState.update { it.copy(enableErrorRes = null) }
    }
}
