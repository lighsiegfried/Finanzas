package com.kratt.finanzas.presentation.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.usecase.csv.ImportFileError
import com.kratt.finanzas.domain.usecase.csv.ImportPreview
import com.kratt.finanzas.domain.usecase.csv.ImportRowError
import com.kratt.finanzas.presentation.common.containerViewModel

@Composable
fun ImportRoute(onBack: () -> Unit, onOpenBackup: () -> Unit) {
    val viewModel = containerViewModel { ImportViewModel(it.csvImporter) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.messageRes) {
        state.messageRes?.let { snackbarHostState.showSnackbar(context.getString(it)); viewModel.onMessageShown() }
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onFileSelected(uri, resolver)
    }
    val templateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.onWriteTemplate(uri, resolver)
    }

    ImportScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSelectFile = { pickLauncher.launch(arrayOf("text/*")) },
        onDownloadTemplate = { templateLauncher.launch("plantilla-mis-finanzas.csv") },
        onConfirmImport = viewModel::onConfirmImport,
        onOpenBackup = onOpenBackup,
        onCancel = { viewModel.onCancel(); onBack() },
        onFinish = { viewModel.reset(); onBack() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    state: ImportUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSelectFile: () -> Unit,
    onDownloadTemplate: () -> Unit,
    onConfirmImport: (Boolean) -> Unit,
    onOpenBackup: () -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit,
) {
    var showFormat by remember { mutableStateOf(false) }
    var showDuplicates by remember { mutableStateOf(false) }
    var showBackup by remember { mutableStateOf(false) }
    var skipDuplicates by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.testTag(TestTags.IMPORT_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.import_title)) },
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
            val summary = state.summary
            when {
                state.loading -> CircularProgressIndicator()
                summary != null -> ImportSummaryCard(summary, onFinish)
                state.preview != null -> ImportPreviewCard(
                    preview = state.preview,
                    onImport = {
                        if (state.preview.duplicateCount > 0) showDuplicates = true else { skipDuplicates = false; showBackup = true }
                    },
                    onCancel = onCancel,
                )
                else -> {
                    Text(stringResource(R.string.import_explanation), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onSelectFile, modifier = Modifier.fillMaxWidth().testTag(TestTags.SELECT_CSV_BUTTON)) {
                        Text(stringResource(R.string.select_csv))
                    }
                    OutlinedButton(onClick = onDownloadTemplate, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.download_template))
                    }
                    TextButton(onClick = { showFormat = true }) { Text(stringResource(R.string.view_format)) }
                }
            }
        }
    }

    if (showFormat) {
        AlertDialog(
            onDismissRequest = { showFormat = false },
            title = { Text(stringResource(R.string.import_format_title)) },
            text = { Text(stringResource(R.string.import_format_body)) },
            confirmButton = { TextButton(onClick = { showFormat = false }) { Text(stringResource(R.string.accept_action)) } },
        )
    }

    if (showDuplicates) {
        AlertDialog(
            onDismissRequest = { showDuplicates = false },
            title = { Text(stringResource(R.string.duplicates_detected_title)) },
            text = { Text(stringResource(R.string.possible_duplicates) + ": " + (state.preview?.duplicateCount ?: 0)) },
            confirmButton = {
                TextButton(onClick = { skipDuplicates = false; showDuplicates = false; showBackup = true }) {
                    Text(stringResource(R.string.import_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = { skipDuplicates = true; showDuplicates = false; showBackup = true }) {
                    Text(stringResource(R.string.skip_duplicates))
                }
            },
        )
    }

    if (showBackup) {
        AlertDialog(
            onDismissRequest = { showBackup = false },
            title = { Text(stringResource(R.string.backup_before_import)) },
            text = { Text(stringResource(R.string.backup_before_import_message)) },
            confirmButton = {
                TextButton(onClick = { showBackup = false; onOpenBackup() }) {
                    Text(stringResource(R.string.backup_before_import))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackup = false; onConfirmImport(skipDuplicates) },
                    modifier = Modifier.testTag(TestTags.CONFIRM_IMPORT_BUTTON),
                ) { Text(stringResource(R.string.continue_without_backup)) }
            },
        )
    }
}

@Composable
private fun ImportPreviewCard(preview: ImportPreview, onImport: () -> Unit, onCancel: () -> Unit) {
    Text(stringResource(R.string.import_preview_title), style = MaterialTheme.typography.titleMedium)
    val fileError = preview.fileError
    if (fileError != null) {
        Text(stringResource(fileErrorRes(fileError)), color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
        return
    }
    Card(modifier = Modifier.fillMaxWidth().testTag(TestTags.IMPORT_PREVIEW)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CountRow(stringResource(R.string.valid_rows), preview.validCount)
            CountRow(stringResource(R.string.possible_duplicates), preview.duplicateCount)
            CountRow(stringResource(R.string.error_rows), preview.errorCount)
            if (preview.errors.isNotEmpty()) {
                HorizontalDivider()
                preview.errors.take(20).forEach { err ->
                    Text(stringResource(R.string.import_row_format, err.rowNumber, stringResource(rowErrorRes(err.error))), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    Button(onClick = onImport, enabled = preview.canImport, modifier = Modifier.fillMaxWidth().testTag(TestTags.IMPORT_BUTTON)) {
        Text(stringResource(R.string.import_action))
    }
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel_action)) }
}

@Composable
private fun ImportSummaryCard(summary: com.kratt.finanzas.domain.usecase.csv.ImportSummary, onFinish: () -> Unit) {
    Text(stringResource(R.string.import_completed), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag(TestTags.IMPORT_SUMMARY))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CountRow(stringResource(R.string.imported_movements), summary.imported)
            CountRow(stringResource(R.string.skipped_rows), summary.skippedDuplicates)
            CountRow(stringResource(R.string.error_rows), summary.errors)
            CountRow(stringResource(R.string.duplicates_detected_label), summary.duplicatesDetected)
        }
    }
    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_finish)) }
}

@Composable
private fun CountRow(label: String, value: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.toString(), style = MaterialTheme.typography.bodyLarge)
    }
}

private fun fileErrorRes(error: ImportFileError): Int = when (error) {
    ImportFileError.EMPTY -> R.string.error_file_empty
    ImportFileError.MISSING_REQUIRED_COLUMN -> R.string.error_missing_column
    ImportFileError.TOO_LARGE -> R.string.error_file_too_large
}

private fun rowErrorRes(error: ImportRowError): Int = when (error) {
    ImportRowError.MALFORMED_ROW -> R.string.error_malformed_row
    ImportRowError.INVALID_TYPE -> R.string.error_invalid_type
    ImportRowError.INVALID_DATE -> R.string.error_invalid_date_import
    ImportRowError.INVALID_AMOUNT -> R.string.error_invalid_amount_import
    ImportRowError.AMOUNT_TOO_LARGE -> R.string.error_amount_too_large
    ImportRowError.ACCOUNT_NOT_FOUND -> R.string.error_account_not_found
    ImportRowError.MISSING_CATEGORY -> R.string.error_missing_category_import
    ImportRowError.CATEGORY_NOT_FOUND -> R.string.error_category_not_found
    ImportRowError.MISSING_DESTINATION -> R.string.error_missing_destination
    ImportRowError.DESTINATION_NOT_FOUND -> R.string.error_destination_not_found
    ImportRowError.SAME_ACCOUNT -> R.string.error_accounts_differ
}
