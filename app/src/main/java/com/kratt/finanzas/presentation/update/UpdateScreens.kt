package com.kratt.finanzas.presentation.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags

// aviso no bloqueante tras una actualizacion exitosa
@Composable
fun UpdateSuccessDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.UPDATE_SUCCESS_DIALOG),
        title = { Text(stringResource(R.string.update_success_title)) },
        text = { Text(stringResource(R.string.update_success_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_success_ok)) }
        },
    )
}

// pantalla de fallo de actualizacion; nunca ofrece una accion destructiva ni borra datos
@Composable
fun UpdateFailureRoute(onRetry: () -> Unit, onCreateDiagnostic: () -> Unit) {
    var showHelp by remember { mutableStateOf(false) }
    Scaffold(modifier = Modifier.fillMaxSize().testTag(TestTags.UPDATE_FAILURE_SCREEN)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.update_failure_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.update_failure_message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().testTag(TestTags.UPDATE_RETRY)) {
                Text(stringResource(R.string.update_retry))
            }
            OutlinedButton(
                onClick = onCreateDiagnostic,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.UPDATE_DIAGNOSTIC),
            ) {
                Text(stringResource(R.string.update_create_diagnostic))
            }
            TextButton(onClick = { showHelp = !showHelp }, modifier = Modifier.testTag(TestTags.UPDATE_HELP)) {
                Text(stringResource(R.string.update_view_help))
            }
            if (showHelp) {
                Text(
                    text = stringResource(R.string.update_help_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
