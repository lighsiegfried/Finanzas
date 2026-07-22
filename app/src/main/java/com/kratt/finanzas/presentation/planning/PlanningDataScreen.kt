package com.kratt.finanzas.presentation.planning

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.data.csv.PlanningCsvExporter
import com.kratt.finanzas.data.csv.PlanningCsvImporter
import com.kratt.finanzas.data.report.CsvExporter
import com.kratt.finanzas.data.report.CsvTable
import com.kratt.finanzas.data.repository.PlannedPurchaseRepository
import com.kratt.finanzas.data.repository.SavingsGoalRepository
import com.kratt.finanzas.domain.usecase.csv.PlanningImportPreview
import com.kratt.finanzas.presentation.common.containerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlanningDataUiState(
    val preview: PlanningImportPreview? = null,
    val message: String? = null,
)

class PlanningDataViewModel(
    private val goalRepo: SavingsGoalRepository,
    private val purchaseRepo: PlannedPurchaseRepository,
    private val importer: PlanningCsvImporter,
    private val csvExporter: CsvExporter,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanningDataUiState())
    val state: StateFlow<PlanningDataUiState> = _state.asStateFlow()

    fun onExportGoals(uri: Uri, resolver: ContentResolver, doneMsg: String) = viewModelScope.launch {
        val goals = goalRepo.observeAll().first()
        val totals = goalRepo.observeTotalsByGoal().first().associate { it.savingsGoalId to it.totalCents }
        writeTable(uri, resolver, PlanningCsvExporter.goalsTable(goals, totals), doneMsg)
    }

    fun onExportPurchases(uri: Uri, resolver: ContentResolver, doneMsg: String) = viewModelScope.launch {
        val purchases = purchaseRepo.observeAll().first()
        writeTable(uri, resolver, PlanningCsvExporter.purchasesTable(purchases), doneMsg)
    }

    private suspend fun writeTable(uri: Uri, resolver: ContentResolver, table: CsvTable, doneMsg: String) {
        val ok = withContext(Dispatchers.IO) {
            runCatching { resolver.openOutputStream(uri)?.use { csvExporter.write(it, table) } != null }.getOrDefault(false)
        }
        if (ok) _state.value = _state.value.copy(message = doneMsg)
    }

    // analiza el archivo elegido y guarda la vista previa
    fun onPreview(uri: Uri, resolver: ContentResolver) = viewModelScope.launch {
        val preview = withContext(Dispatchers.IO) {
            runCatching { resolver.openInputStream(uri)?.use { importer.preview(it) } }.getOrNull()
        }
        _state.value = _state.value.copy(preview = preview)
    }

    // importa las filas validas en una sola operacion
    fun onImport(doneMsg: (Int) -> String) = viewModelScope.launch {
        val preview = _state.value.preview ?: return@launch
        val count = withContext(Dispatchers.IO) { runCatching { importer.commit(preview) }.getOrDefault(0) }
        _state.value = PlanningDataUiState(message = doneMsg(count))
    }

    fun onMessageShown() { _state.value = _state.value.copy(message = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningDataRoute(onBack: () -> Unit) {
    val viewModel = containerViewModel {
        PlanningDataViewModel(it.savingsGoalRepository, it.plannedPurchaseRepository, it.planningCsvImporter, it.csvExporter)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.onMessageShown() }
    }

    val goalsFile = "mis-finanzas-metas.csv"
    val purchasesFile = "mis-finanzas-compras.csv"
    val exportDone = stringResource(R.string.planning_export_done)
    val exportGoals = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.onExportGoals(uri, resolver, exportDone)
    }
    val exportPurchases = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.onExportPurchases(uri, resolver, exportDone)
    }
    val importPick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onPreview(uri, resolver)
    }
    // plantilla del mensaje de exito; se formatea con la cantidad al importar
    val doneTemplate = stringResource(R.string.planning_import_done)

    Scaffold(
        modifier = Modifier.testTag(TestTags.PLANNING_CSV_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.planning_csv_title)) },
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
            Text(stringResource(R.string.planning_import_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { exportGoals.launch(goalsFile) }, modifier = Modifier.fillMaxWidth().testTag(TestTags.EXPORT_GOALS_BUTTON)) {
                Text(stringResource(R.string.planning_export_goals))
            }
            OutlinedButton(onClick = { exportPurchases.launch(purchasesFile) }, modifier = Modifier.fillMaxWidth().testTag(TestTags.EXPORT_PURCHASES_BUTTON)) {
                Text(stringResource(R.string.planning_export_purchases))
            }
            Button(onClick = { importPick.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth().testTag(TestTags.IMPORT_PLANNING_BUTTON)) {
                Text(stringResource(R.string.planning_import))
            }

            val preview = state.preview
            if (preview != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (preview.fileError) {
                            Text(stringResource(R.string.planning_import_invalid), color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(stringResource(R.string.planning_import_summary, preview.validCount, preview.errorRows.size, preview.duplicateRows.size))
                            if (preview.hasImportable) {
                                Button(
                                    onClick = { viewModel.onImport { count -> doneTemplate.format(count) } },
                                    modifier = Modifier.fillMaxWidth().testTag(TestTags.CONFIRM_IMPORT_PLANNING_BUTTON),
                                ) {
                                    Text(stringResource(R.string.planning_import_confirm, preview.validCount))
                                }
                            } else {
                                Text(stringResource(R.string.planning_import_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
