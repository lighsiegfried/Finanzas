package com.kratt.finanzas.presentation.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.di.AppViewModelProvider
import com.kratt.finanzas.domain.model.LockTimeout
import com.kratt.finanzas.security.AuthError
import com.kratt.finanzas.security.BiometricAuthenticator

@Composable
fun SecuritySettingsRoute(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    viewModel: SecuritySettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as FragmentActivity
    val title = stringResource(R.string.unlock_prompt_title)
    val subtitle = stringResource(R.string.unlock_prompt_subtitle)

    // al volver de los ajustes del sistema se reevalua la disponibilidad
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAvailability()
    }

    SecuritySettingsScreen(
        state = state,
        onToggleLock = { wantEnabled ->
            if (wantEnabled) {
                // activar el bloqueo exige confirmar identidad con el sistema
                BiometricAuthenticator.authenticate(
                    activity = activity,
                    title = title,
                    subtitle = subtitle,
                    onSuccess = viewModel::onLockEnableConfirmed,
                    onError = { error ->
                        if (error == AuthError.FAILED || error == AuthError.LOCKOUT) {
                            viewModel.onEnableAuthFailed()
                        }
                    },
                )
            } else {
                viewModel.onLockDisabled()
            }
        },
        onTimeoutSelected = viewModel::onTimeoutSelected,
        onEnableErrorShown = viewModel::onEnableErrorShown,
        onBack = onBack,
        onOpenBackup = onOpenBackup,
    )
}

// pantalla de ajustes de seguridad, no muestra ningun dato financiero
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    state: SecuritySettingsUiState,
    onToggleLock: (Boolean) -> Unit,
    onTimeoutSelected: (LockTimeout) -> Unit,
    onEnableErrorShown: () -> Unit,
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.enableErrorRes) {
        state.enableErrorRes?.let { res ->
            snackbarHostState.showSnackbar(context.getString(res))
            onEnableErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.testTag(TestTags.SECURITY_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.security_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
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
        ) {
            Text(
                text = stringResource(R.string.app_lock_section),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.use_biometric_or_device_lock),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = state.lockEnabled,
                    onCheckedChange = onToggleLock,
                    enabled = state.authAvailable && !state.isLoading,
                    modifier = Modifier.testTag(TestTags.LOCK_SWITCH),
                )
            }
            if (!state.authAvailable) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.device_lock_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(TestTags.LOCK_UNAVAILABLE_MESSAGE),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.auto_lock_section),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TimeoutOption(
                labelRes = R.string.timeout_session,
                supportingRes = R.string.timeout_session_supporting,
                selected = state.timeout == LockTimeout.SESSION,
                enabled = state.lockEnabled,
                onSelected = { onTimeoutSelected(LockTimeout.SESSION) },
                tag = TestTags.TIMEOUT_SESSION,
            )
            TimeoutOption(
                labelRes = R.string.timeout_ten_minutes,
                supportingRes = R.string.timeout_ten_minutes_supporting,
                selected = state.timeout == LockTimeout.TEN_MINUTES,
                enabled = state.lockEnabled,
                onSelected = { onTimeoutSelected(LockTimeout.TEN_MINUTES) },
                tag = TestTags.TIMEOUT_TEN_MINUTES,
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenBackup)
                    .padding(vertical = 16.dp)
                    .testTag(TestTags.BACKUP_SETTINGS_ENTRY),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.backup_title),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun TimeoutOption(
    labelRes: Int,
    supportingRes: Int,
    selected: Boolean,
    enabled: Boolean,
    onSelected: () -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, onClick = onSelected)
            .padding(vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            // texto de apoyo que explica que hace cada opcion
            Text(
                text = stringResource(supportingRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
