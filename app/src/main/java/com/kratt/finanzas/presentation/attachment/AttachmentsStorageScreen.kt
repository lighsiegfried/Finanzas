package com.kratt.finanzas.presentation.attachment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.domain.model.AttachmentStorageSummary
import com.kratt.finanzas.domain.usecase.AttachmentStorageSummaryUseCase
import com.kratt.finanzas.presentation.common.containerViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

// resumen del espacio usado por los adjuntos, calculado desde los metadatos, sin escanear el sistema
class AttachmentsStorageViewModel(
    summaryUseCase: AttachmentStorageSummaryUseCase,
) : ViewModel() {
    val summary: StateFlow<AttachmentStorageSummary> =
        summaryUseCase().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AttachmentStorageSummary(0, 0))
}

@Composable
fun AttachmentsStorageRoute(onBack: () -> Unit) {
    val viewModel = containerViewModel { AttachmentsStorageViewModel(it.attachmentStorageSummary) }
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    AttachmentsStorageScreen(summary = summary, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentsStorageScreen(
    summary: AttachmentStorageSummary,
    onBack: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.attachments_storage_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow(stringResource(R.string.attachments_storage_count), summary.fileCount.toString())
                    SummaryRow(stringResource(R.string.attachments_storage_total), formatAttachmentSize(summary.totalBytes))
                }
            }
            Text(
                text = stringResource(R.string.attachments_storage_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

// formatea el tamano total en unidades legibles
fun formatAttachmentSize(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val locale = Locale.forLanguageTag("es-GT")
    return when {
        bytes >= mb -> String.format(locale, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(locale, "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}
