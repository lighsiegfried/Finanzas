package com.kratt.finanzas.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags

// estados reutilizables en espanol; nunca dejan una pantalla en blanco ni muestran excepciones crudas

// indicador de carga con texto, solo cuando de verdad hay una operacion en curso
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.state_loading),
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp).testTag(TestTags.STATE_LOADING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(text = message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

// estado vacio con mensaje claro y sin ruido visual
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.state_empty),
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(24.dp).testTag(TestTags.STATE_EMPTY),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// estado de error con opcion de reintentar cuando es util
@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.state_error),
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp).testTag(TestTags.STATE_ERROR),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry, modifier = Modifier.testTag(TestTags.STATE_RETRY_BUTTON)) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
