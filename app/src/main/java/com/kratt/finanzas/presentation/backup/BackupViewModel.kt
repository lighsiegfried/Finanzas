package com.kratt.finanzas.presentation.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.data.backup.BackupMetadata
import com.kratt.finanzas.data.backup.BackupPassword
import com.kratt.finanzas.data.backup.RestoreCandidate
import com.kratt.finanzas.data.backup.RestoreOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

enum class BackupStage {
    IDLE,
    CREATE_PASSWORD,
    EXPORT_CONFIRM,
    RESTORE_PASSWORD,
    RESTORE_CONFIRM,
    WORKING,
    SUCCESS,
    ERROR,
}

data class BackupUiState(
    val lastBackupMillis: Long? = null,
    val hasBackup: Boolean = false,
    val stage: BackupStage = BackupStage.IDLE,
    val password: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    @StringRes val passwordErrorRes: Int? = null,
    @StringRes val workingMessageRes: Int? = null,
    @StringRes val resultTitleRes: Int? = null,
    @StringRes val resultMessageRes: Int? = null,
    // opcion opcional para incluir los adjuntos cifrados en el respaldo
    val includeAttachments: Boolean = false,
)

class BackupViewModel(
    metadata: Flow<BackupMetadata>,
    private val createBackup: suspend (OutputStream, ByteArray, Boolean) -> Boolean,
    private val prepareRestore: suspend (Uri, ContentResolver, ByteArray) -> RestoreOutcome,
    private val commitRestore: suspend (RestoreCandidate) -> Boolean,
    private val discardRestore: (RestoreCandidate) -> Unit,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    // frase importada validada, vive solo en memoria hasta confirmar o cancelar
    private var pendingCandidate: RestoreCandidate? = null
    private var pendingRestoreUri: Uri? = null

    init {
        viewModelScope.launch {
            metadata.collect { meta: BackupMetadata ->
                _uiState.update { it.copy(lastBackupMillis = meta.lastBackupMillis, hasBackup = meta.hasBackup) }
            }
        }
    }

    fun onPasswordChange(value: String) {
        if (BackupPassword.codePoints(value) > BackupPassword.MAX_CODE_POINTS) return
        _uiState.update { it.copy(password = value, passwordErrorRes = null) }
    }

    fun onConfirmChange(value: String) {
        if (BackupPassword.codePoints(value) > BackupPassword.MAX_CODE_POINTS) return
        _uiState.update { it.copy(confirmPassword = value, passwordErrorRes = null) }
    }

    fun onToggleShowPassword() {
        _uiState.update { it.copy(showPassword = !it.showPassword) }
    }

    fun onToggleIncludeAttachments() {
        _uiState.update { it.copy(includeAttachments = !it.includeAttachments) }
    }

    fun onCreateBackupClick() {
        _uiState.update { it.copy(stage = BackupStage.CREATE_PASSWORD, password = "", confirmPassword = "", showPassword = false, passwordErrorRes = null) }
    }

    // valida la contrasena antes de pedir el destino del archivo
    fun onCreatePasswordContinue(): Boolean {
        val state = _uiState.value
        val error = when {
            BackupPassword.isEmpty(state.password) -> R.string.error_empty_password
            BackupPassword.isTooShort(state.password) -> R.string.password_too_short
            !BackupPassword.matches(state.password, state.confirmPassword) -> R.string.password_mismatch
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(passwordErrorRes = error) }
            return false
        }
        _uiState.update { it.copy(stage = BackupStage.EXPORT_CONFIRM) }
        return true
    }

    // ejecuta la exportacion al destino elegido en el selector del sistema
    fun onDestinationChosen(uri: Uri, resolver: ContentResolver) {
        val password = _uiState.value.password.toByteArray(Charsets.UTF_8)
        val includeAttachments = _uiState.value.includeAttachments
        _uiState.update { it.copy(stage = BackupStage.WORKING, workingMessageRes = R.string.backup_protecting) }
        viewModelScope.launch {
            val ok = try {
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri, "wt")?.use { out -> createBackup(out, password, includeAttachments) } ?: false
                }
            } catch (e: Exception) {
                false
            } finally {
                password.fill(0)
            }
            clearPasswordFields()
            if (ok) {
                _uiState.update { it.copy(stage = BackupStage.SUCCESS, resultTitleRes = R.string.backup_created_title, resultMessageRes = R.string.backup_created_message) }
            } else {
                _uiState.update { it.copy(stage = BackupStage.ERROR, resultTitleRes = null, resultMessageRes = R.string.export_error) }
            }
        }
    }

    fun onRestoreSourceChosen(uri: Uri) {
        pendingRestoreUri = uri
        _uiState.update { it.copy(stage = BackupStage.RESTORE_PASSWORD, password = "", showPassword = false, passwordErrorRes = null) }
    }

    // deriva la clave, autentica y valida el respaldo antes de confirmar
    fun onRestorePasswordSubmit(resolver: ContentResolver) {
        val uri = pendingRestoreUri ?: return
        val state = _uiState.value
        if (BackupPassword.isEmpty(state.password)) {
            _uiState.update { it.copy(passwordErrorRes = R.string.error_empty_password) }
            return
        }
        val password = state.password.toByteArray(Charsets.UTF_8)
        _uiState.update { it.copy(stage = BackupStage.WORKING, workingMessageRes = R.string.backup_validating) }
        viewModelScope.launch {
            val outcome = try {
                prepareRestore(uri, resolver, password)
            } catch (e: Exception) {
                RestoreOutcome.WrongPasswordOrCorrupt
            } finally {
                password.fill(0)
            }
            when (outcome) {
                is RestoreOutcome.Valid -> {
                    pendingCandidate = outcome.candidate
                    clearPasswordFields()
                    _uiState.update { it.copy(stage = BackupStage.RESTORE_CONFIRM) }
                }
                RestoreOutcome.WrongPasswordOrCorrupt ->
                    _uiState.update { it.copy(stage = BackupStage.ERROR, resultMessageRes = R.string.wrong_password) }
                RestoreOutcome.Unsupported ->
                    _uiState.update { it.copy(stage = BackupStage.ERROR, resultMessageRes = R.string.wrong_password) }
            }
        }
    }

    // reemplaza la base actual por la del respaldo tras la confirmacion final
    fun onRestoreConfirm() {
        val candidate = pendingCandidate ?: return
        _uiState.update { it.copy(stage = BackupStage.WORKING, workingMessageRes = R.string.backup_validating) }
        viewModelScope.launch {
            val ok = try {
                commitRestore(candidate)
            } catch (e: Exception) {
                false
            }
            pendingCandidate = null
            if (ok) {
                _uiState.update { it.copy(stage = BackupStage.SUCCESS, resultTitleRes = R.string.restore_completed_title, resultMessageRes = R.string.restore_completed_message) }
            } else {
                _uiState.update { it.copy(stage = BackupStage.ERROR, resultMessageRes = R.string.export_error) }
            }
        }
    }

    fun onRestoreCancel() {
        pendingCandidate?.let(discardRestore)
        pendingCandidate = null
        pendingRestoreUri = null
        clearPasswordFields()
        _uiState.update { it.copy(stage = BackupStage.IDLE) }
    }

    fun onDismiss() {
        pendingCandidate?.let(discardRestore)
        pendingCandidate = null
        pendingRestoreUri = null
        clearPasswordFields()
        _uiState.update { it.copy(stage = BackupStage.IDLE, resultTitleRes = null, resultMessageRes = null, passwordErrorRes = null) }
    }

    // limpia los campos de contrasena al salir del flujo
    private fun clearPasswordFields() {
        _uiState.update { it.copy(password = "", confirmPassword = "", showPassword = false, passwordErrorRes = null) }
    }

    override fun onCleared() {
        pendingCandidate?.let(discardRestore)
        pendingCandidate = null
        super.onCleared()
    }
}
