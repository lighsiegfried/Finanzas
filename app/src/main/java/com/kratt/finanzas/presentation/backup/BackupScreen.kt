package com.kratt.finanzas.presentation.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.data.backup.BackupFileName
import com.kratt.finanzas.di.AppViewModelProvider
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BackupRoute(
    onBack: () -> Unit,
    viewModel: BackupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupFileName.MIME_TYPE),
    ) { uri -> if (uri != null) viewModel.onDestinationChosen(uri, resolver) else viewModel.onDismiss() }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.onRestoreSourceChosen(uri) else viewModel.onDismiss() }

    BackupScreen(
        state = state,
        onBack = onBack,
        onCreateClick = viewModel::onCreateBackupClick,
        onRestoreClick = { openLauncher.launch(arrayOf("*/*")) },
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onToggleShow = viewModel::onToggleShowPassword,
        onToggleIncludeAttachments = viewModel::onToggleIncludeAttachments,
        onCreateContinue = { viewModel.onCreatePasswordContinue() },
        onExportConfirm = { createLauncher.launch(BackupFileName.generate(LocalDateTime.now())) },
        onRestorePasswordSubmit = { viewModel.onRestorePasswordSubmit(resolver) },
        onRestoreConfirm = viewModel::onRestoreConfirm,
        onCancel = viewModel::onRestoreCancel,
        onDismiss = viewModel::onDismiss,
    )
}

// pantalla de respaldo, no muestra montos ni datos financieros
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    state: BackupUiState,
    onBack: () -> Unit,
    onCreateClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onToggleShow: () -> Unit,
    onToggleIncludeAttachments: () -> Unit,
    onCreateContinue: () -> Unit,
    onExportConfirm: () -> Unit,
    onRestorePasswordSubmit: () -> Unit,
    onRestoreConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(TestTags.BACKUP_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.backup_explanation), style = MaterialTheme.typography.bodyMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.backup_last), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = lastBackupText(state),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag(TestTags.LAST_BACKUP_LABEL),
                    )
                }
            }
            Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth().testTag(TestTags.CREATE_BACKUP_BUTTON)) {
                Text(stringResource(R.string.backup_create))
            }
            OutlinedButton(onClick = onRestoreClick, modifier = Modifier.fillMaxWidth().testTag(TestTags.RESTORE_BACKUP_BUTTON)) {
                Text(stringResource(R.string.backup_restore))
            }
            HorizontalDivider()
            Text(stringResource(R.string.backup_password_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.restore_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.forgotten_password_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // educacion sobre desinstalar; el boton reutiliza el flujo normal de crear respaldo
            com.kratt.finanzas.presentation.common.UninstallEducationCard(onCreateBackup = onCreateClick)
        }
    }

    when (state.stage) {
        BackupStage.CREATE_PASSWORD -> CreatePasswordDialog(state, onPasswordChange, onConfirmChange, onToggleShow, onToggleIncludeAttachments, onCreateContinue, onDismiss)
        BackupStage.EXPORT_CONFIRM -> ConfirmDialog(
            titleRes = R.string.export_confirm_title,
            messageRes = null,
            confirmRes = R.string.continue_action,
            confirmTag = TestTags.EXPORT_CONFIRM_BUTTON,
            onConfirm = onExportConfirm,
            onDismiss = onDismiss,
        )
        BackupStage.RESTORE_PASSWORD -> RestorePasswordDialog(state, onPasswordChange, onToggleShow, onRestorePasswordSubmit, onCancel)
        BackupStage.RESTORE_CONFIRM -> ConfirmDialog(
            titleRes = R.string.restore_confirm_title,
            messageRes = R.string.restore_confirm_message,
            confirmRes = R.string.restore_confirm_action,
            confirmTag = TestTags.RESTORE_CONFIRM_BUTTON,
            onConfirm = onRestoreConfirm,
            onDismiss = onCancel,
        )
        BackupStage.WORKING -> WorkingDialog(state)
        BackupStage.SUCCESS, BackupStage.ERROR -> ResultDialog(state, onDismiss)
        BackupStage.IDLE -> Unit
    }
}

@Composable
private fun lastBackupText(state: BackupUiState): String =
    if (!state.hasBackup || state.lastBackupMillis == null) {
        stringResource(R.string.backup_never)
    } else {
        stringResource(R.string.backup_created_on, formatBackupDate(state.lastBackupMillis))
    }

private val backupDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

private fun formatBackupDate(millis: Long): String =
    backupDateFormatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()))

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, show: Boolean, labelRes: Int, tag: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

@Composable
private fun CreatePasswordDialog(
    state: BackupUiState,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onToggleShow: () -> Unit,
    onToggleIncludeAttachments: () -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_create)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordField(state.password, onPasswordChange, state.showPassword, R.string.backup_password_label, TestTags.PASSWORD_FIELD)
                PasswordField(state.confirmPassword, onConfirmChange, state.showPassword, R.string.confirm_password_label, TestTags.CONFIRM_PASSWORD_FIELD)
                TextButton(onClick = onToggleShow, modifier = Modifier.testTag(TestTags.SHOW_PASSWORD_TOGGLE)) {
                    Text(stringResource(R.string.show_password))
                }
                // opcion opcional para incluir los adjuntos cifrados; aumenta el tamano del archivo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.attachments_include_in_backup), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.attachments_include_in_backup_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.includeAttachments,
                        onCheckedChange = { onToggleIncludeAttachments() },
                        modifier = Modifier.testTag(TestTags.BACKUP_INCLUDE_ATTACHMENTS_SWITCH),
                    )
                }
                state.passwordErrorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Text(stringResource(R.string.backup_password_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue, modifier = Modifier.testTag(TestTags.PASSWORD_CONTINUE_BUTTON)) {
                Text(stringResource(R.string.continue_action))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) } },
    )
}

@Composable
private fun RestorePasswordDialog(
    state: BackupUiState,
    onPasswordChange: (String) -> Unit,
    onToggleShow: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.backup_restore)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordField(state.password, onPasswordChange, state.showPassword, R.string.backup_password_label, TestTags.PASSWORD_FIELD)
                TextButton(onClick = onToggleShow, modifier = Modifier.testTag(TestTags.SHOW_PASSWORD_TOGGLE)) {
                    Text(stringResource(R.string.show_password))
                }
                state.passwordErrorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, modifier = Modifier.testTag(TestTags.RESTORE_PASSWORD_SUBMIT)) {
                Text(stringResource(R.string.continue_action))
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel_action)) } },
    )
}

@Composable
private fun ConfirmDialog(
    titleRes: Int,
    messageRes: Int?,
    confirmRes: Int,
    confirmTag: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = messageRes?.let { { Text(stringResource(it)) } },
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.testTag(confirmTag)) { Text(stringResource(confirmRes)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) } },
    )
}

@Composable
private fun WorkingDialog(state: BackupUiState) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(state.workingMessageRes ?: R.string.backup_protecting)) },
        text = { CircularProgressIndicator() },
        confirmButton = {},
    )
}

@Composable
private fun ResultDialog(state: BackupUiState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { state.resultTitleRes?.let { Text(stringResource(it)) } },
        text = { state.resultMessageRes?.let { Text(stringResource(it)) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.accept_action)) } },
    )
}
