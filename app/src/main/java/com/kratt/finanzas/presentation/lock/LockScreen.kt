package com.kratt.finanzas.presentation.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.di.AppViewModelProvider
import com.kratt.finanzas.security.BiometricAuthenticator

@Composable
fun LockRoute(
    viewModel: LockViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as FragmentActivity
    val title = stringResource(R.string.unlock_prompt_title)
    val subtitle = stringResource(R.string.unlock_prompt_subtitle)

    // al volver de los ajustes del sistema se reevalua la disponibilidad
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAvailability()
    }

    LockScreen(
        state = state,
        onUnlockClick = {
            viewModel.onAuthenticationStarted()
            BiometricAuthenticator.authenticate(
                activity = activity,
                title = title,
                subtitle = subtitle,
                onSuccess = viewModel::onAuthenticationSucceeded,
                onError = viewModel::onAuthenticationError,
            )
        },
        onContinueClick = viewModel::onContinueWithoutAuth,
    )
}

// pantalla de bloqueo, nunca muestra saldos ni movimientos
@Composable
fun LockScreen(
    state: LockUiState,
    onUnlockClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .testTag(TestTags.LOCK_SCREEN),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = stringResource(R.string.app_locked_icon),
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.unlock_prompt_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        state.messageRes?.let { messageRes ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.status == LockScreenStatus.FAILED) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(TestTags.LOCK_MESSAGE),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (state.status == LockScreenStatus.UNAVAILABLE) {
            Button(
                onClick = onContinueClick,
                modifier = Modifier.testTag(TestTags.CONTINUE_BUTTON),
            ) {
                Text(stringResource(R.string.continue_action))
            }
        } else {
            Button(
                onClick = onUnlockClick,
                enabled = state.status != LockScreenStatus.AUTHENTICATING,
                modifier = Modifier.testTag(TestTags.UNLOCK_BUTTON),
            ) {
                Text(stringResource(R.string.unlock_action))
            }
        }
    }
}
