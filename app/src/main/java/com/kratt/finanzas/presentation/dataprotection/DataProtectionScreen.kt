package com.kratt.finanzas.presentation.dataprotection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.update.UpdateStatus
import com.kratt.finanzas.navigation.Destinations
import com.kratt.finanzas.presentation.common.DataActionsExplainer
import com.kratt.finanzas.presentation.common.UninstallEducationCard
import com.kratt.finanzas.presentation.common.containerViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DataProtectionRoute(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    // registra que se mostro la educacion de desinstalar (marca no sensible)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        (context.applicationContext as com.kratt.finanzas.FinanzasApplication).container.markUninstallWarningShown()
    }
    val viewModel = containerViewModel {
        DataProtectionViewModel(
            databaseState = it.databaseState,
            backupMetadata = it.backupPreferencesRepository.metadata,
            updateStatus = it.updateStatus,
            prepareRestore = it::prepareRestore,
            discardRestore = it::discardRestore,
        )
    }
    val status by viewModel.status.collectAsStateWithLifecycle()
    val verify by viewModel.verify.collectAsStateWithLifecycle()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var askPassword by remember { mutableStateOf(false) }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingUri = uri
            askPassword = true
        }
    }

    DataProtectionScreen(
        status = status,
        verify = verify,
        onBack = onBack,
        onCreateBackup = { onNavigate(Destinations.BACKUP) },
        onMigrate = { onNavigate(Destinations.MIGRATE_PHONE) },
        onRestore = { onNavigate(Destinations.BACKUP) },
        onInstructions = { onNavigate(Destinations.MIGRATE_PHONE) },
        onVerify = { openLauncher.launch(arrayOf("*/*")) },
        onDismissVerifyResult = viewModel::clearVerifyResult,
    )

    if (askPassword) {
        VerifyPasswordDialog(
            onDismiss = { askPassword = false; pendingUri = null },
            onConfirm = { password ->
                val uri = pendingUri
                askPassword = false
                pendingUri = null
                if (uri != null) {
                    val input = runCatching { resolver.openInputStream(uri) }.getOrNull()
                    if (input != null) viewModel.verifyBackup(input, password.encodeToByteArray())
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataProtectionScreen(
    status: DataProtectionStatus,
    verify: VerifyUi,
    onBack: () -> Unit,
    onCreateBackup: () -> Unit,
    onMigrate: () -> Unit,
    onRestore: () -> Unit,
    onInstructions: () -> Unit,
    onVerify: () -> Unit,
    onDismissVerifyResult: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.DATA_PROTECTION_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.data_protection_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCard(status)
            if (!status.hasBackup) NoBackupWarning() else if (status.backupStale) StaleBackupWarning(onCreateBackup)

            Button(onClick = onCreateBackup, modifier = Modifier.fillMaxWidth().testTag(TestTags.DATA_PROTECTION_BACKUP)) {
                Text(stringResource(R.string.dp_action_create))
            }
            OutlinedButton(onClick = onMigrate, modifier = Modifier.fillMaxWidth().testTag(TestTags.DATA_PROTECTION_MIGRATE)) {
                Text(stringResource(R.string.dp_action_migrate))
            }
            OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth().testTag(TestTags.DATA_PROTECTION_RESTORE)) {
                Text(stringResource(R.string.dp_action_restore))
            }
            OutlinedButton(onClick = onVerify, modifier = Modifier.fillMaxWidth().testTag(TestTags.DATA_PROTECTION_VERIFY)) {
                Text(stringResource(R.string.dp_action_verify))
            }
            TextButton(onClick = onInstructions, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dp_action_instructions))
            }

            UninstallEducationCard(onCreateBackup = onCreateBackup)
            DataActionsExplainer()
        }
    }

    if (verify.result != null) {
        val message = when (verify.result) {
            BackupVerifyResult.VALID -> R.string.dp_verify_valid
            BackupVerifyResult.INVALID -> R.string.dp_verify_invalid
            BackupVerifyResult.UNSUPPORTED -> R.string.dp_verify_unsupported
        }
        AlertDialog(
            onDismissRequest = onDismissVerifyResult,
            title = { Text(stringResource(R.string.dp_verify_title)) },
            text = { Text(stringResource(message)) },
            confirmButton = { TextButton(onClick = onDismissVerifyResult) { Text(stringResource(R.string.update_success_ok)) } },
        )
    }
}

@Composable
private fun StatusCard(status: DataProtectionStatus) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.dp_status_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(if (status.dbReady) R.string.dp_db_ready else R.string.dp_db_not_ready),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(R.string.dp_encryption), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.dp_schema_version, status.schemaVersion), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.dp_backup_format, status.backupFormatVersion), style = MaterialTheme.typography.bodySmall)
            Text(
                text = stringResource(if (status.hasBackup) R.string.dp_backup_exists_yes else R.string.dp_backup_exists_no),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (status.hasBackup && status.lastBackupMillis != null) {
                Text(stringResource(R.string.dp_last_backup, formatDate(status.lastBackupMillis)), style = MaterialTheme.typography.bodySmall)
                val age = status.backupAgeDays
                if (age != null) {
                    val ageText = if (age <= 0) stringResource(R.string.dp_backup_age_today) else stringResource(R.string.dp_backup_age_days, age)
                    Text(ageText, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                text = stringResource(
                    if (status.updateStatus == UpdateStatus.SUCCESS) R.string.dp_update_status_ok else R.string.dp_update_status_none,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoBackupWarning() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.no_backup_warning_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.no_backup_warning_message), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StaleBackupWarning(onCreateBackup: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.backup_stale_warning), style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = onCreateBackup) { Text(stringResource(R.string.backup_stale_action)) }
        }
    }
}

@Composable
private fun VerifyPasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dp_verify_title)) },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.dp_verify_password_hint)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = password.isNotBlank()) {
                Text(stringResource(R.string.dp_verify_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.assistant_cancel)) } },
    )
}

// formatea la marca de tiempo del respaldo como fecha local legible
private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
