package com.kratt.finanzas.presentation.dataprotection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.backup.BackupHeader
import com.kratt.finanzas.data.backup.BackupManifest
import com.kratt.finanzas.data.backup.BackupMetadata
import com.kratt.finanzas.data.backup.RestoreCandidate
import com.kratt.finanzas.data.backup.RestoreOutcome
import com.kratt.finanzas.di.DatabaseBootstrapState
import com.kratt.finanzas.domain.update.BackupFreshness
import com.kratt.finanzas.domain.update.UpdateStatus
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BackupVerifyResult { VALID, INVALID, UNSUPPORTED }

// estado de solo lectura del centro de proteccion de datos; nunca expone claves ni rutas
data class DataProtectionStatus(
    val dbReady: Boolean,
    val schemaVersion: Int,
    val backupFormatVersion: Int,
    val hasBackup: Boolean,
    val lastBackupMillis: Long?,
    val backupAgeDays: Long?,
    val backupStale: Boolean,
    val updateStatus: UpdateStatus,
)

data class VerifyUi(val working: Boolean = false, val result: BackupVerifyResult? = null)

class DataProtectionViewModel(
    databaseState: StateFlow<DatabaseBootstrapState>,
    backupMetadata: Flow<BackupMetadata>,
    updateStatus: StateFlow<UpdateStatus>,
    private val prepareRestore: suspend (InputStream, ByteArray) -> RestoreOutcome,
    private val discardRestore: (RestoreCandidate) -> Unit,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val status: StateFlow<DataProtectionStatus> = combine(databaseState, backupMetadata, updateStatus) { db, meta, upd ->
        val now = nowMillis()
        DataProtectionStatus(
            dbReady = db == DatabaseBootstrapState.READY,
            schemaVersion = BackupManifest.CURRENT_ROOM_SCHEMA,
            backupFormatVersion = BackupHeader.FORMAT_VERSION,
            hasBackup = meta.hasBackup,
            lastBackupMillis = meta.lastBackupMillis,
            backupAgeDays = BackupFreshness.ageDays(meta.lastBackupMillis, now),
            backupStale = BackupFreshness.isStale(meta.lastBackupMillis, now),
            updateStatus = upd,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DataProtectionStatus(false, BackupManifest.CURRENT_ROOM_SCHEMA, BackupHeader.FORMAT_VERSION, false, null, null, false, UpdateStatus.NONE),
    )

    private val _verify = MutableStateFlow(VerifyUi())
    val verify: StateFlow<VerifyUi> = _verify.asStateFlow()

    // valida un respaldo elegido sin reemplazar nada; descarta el candidato al terminar
    fun verifyBackup(input: InputStream, password: ByteArray) {
        _verify.value = VerifyUi(working = true)
        viewModelScope.launch(Dispatchers.Default) {
            val result = when (val outcome = prepareRestore(input, password)) {
                is RestoreOutcome.Valid -> {
                    discardRestore(outcome.candidate)
                    BackupVerifyResult.VALID
                }
                RestoreOutcome.WrongPasswordOrCorrupt -> BackupVerifyResult.INVALID
                RestoreOutcome.Unsupported -> BackupVerifyResult.UNSUPPORTED
            }
            password.fill(0)
            _verify.value = VerifyUi(result = result)
        }
    }

    fun clearVerifyResult() {
        _verify.value = VerifyUi()
    }
}
