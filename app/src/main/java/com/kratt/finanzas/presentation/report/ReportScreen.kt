package com.kratt.finanzas.presentation.report

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.ReportViewMode
import com.kratt.finanzas.domain.usecase.ReportPeriod
import com.kratt.finanzas.presentation.charts.ChartPoint
import com.kratt.finanzas.presentation.charts.InteractiveChartCard
import com.kratt.finanzas.presentation.common.LocalBalancesHidden
import com.kratt.finanzas.presentation.common.PeriodSelector
import com.kratt.finanzas.presentation.common.containerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ReportRoute(
    type: ReportType,
    onBack: () -> Unit,
    onOpenMovements: (ReportChartFilter) -> Unit = {},
) {
    val appContext = LocalContext.current.applicationContext
    val resolver = LocalContext.current.contentResolver
    val viewModel = containerViewModel(key = "report_${type.name}") {
        ReportViewModel(appContext, it.reportRepository, it.budgetRepository, it.installmentRepository, it.recurringRepository, it.savingsGoalRepository, it.savingsContributionRepository, it.plannedPurchaseRepository, it.csvExporter, it.displayPreferences, type)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val viewMode by viewModel.reportViewMode.collectAsStateWithLifecycle()
    val exportStatus by viewModel.exportStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportedMsg = stringResource(R.string.report_exported)
    val exportErrorMsg = stringResource(R.string.export_error_report)
    LaunchedEffect(exportStatus) {
        when (exportStatus) {
            ReportExport.SUCCESS -> { snackbarHostState.showSnackbar(exportedMsg); viewModel.onExportShown() }
            ReportExport.ERROR -> { snackbarHostState.showSnackbar(exportErrorMsg); viewModel.onExportShown() }
            null -> {}
        }
    }

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.onExport(uri, resolver)
    }
    val fileName = "reporte-finanzas-${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}.csv"

    ReportScreen(
        type = type,
        state = state,
        viewMode = viewMode,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onPeriod = viewModel::onPeriod,
        onCustomRange = viewModel::onCustomRange,
        onViewMode = viewModel::onReportViewMode,
        onOpenMovements = onOpenMovements,
        onExport = { createLauncher.launch(fileName) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    type: ReportType,
    state: ReportUiState,
    viewMode: ReportViewMode,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPeriod: (ReportPeriod) -> Unit,
    onCustomRange: (LocalDate, LocalDate) -> Unit,
    onViewMode: (ReportViewMode) -> Unit,
    onOpenMovements: (ReportChartFilter) -> Unit,
    onExport: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.REPORT_DETAIL_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(type.labelRes())) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val render = state.render
        val hidden = LocalBalancesHidden.current
        // que se muestra segun el modo elegido; los reportes sin grafica siempre muestran la lista
        val hasChartType = render.chartType != ReportChart.NONE
        val showChart = hasChartType && viewMode != ReportViewMode.LIST
        val showTable = !hasChartType || viewMode != ReportViewMode.CHART
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PeriodSelector(selected = state.period, onSelected = onPeriod)
            if (state.period == ReportPeriod.CUSTOM) {
                CustomRangePickers(state, onCustomRange)
            }

            ViewModeToggle(viewMode, onViewMode)

            // grafica interactiva local; el detalle usa montos del dominio, no coordenadas
            if (showChart) {
                val points = render.chartLabels.mapIndexed { index, label ->
                    ChartPoint(label, render.chartAmountsCents.getOrElse(index) { 0L })
                }
                val onView: ((Int) -> Unit)? = if (render.chartFilters.isNotEmpty()) {
                    { index -> render.chartFilters.getOrNull(index)?.let(onOpenMovements) }
                } else null
                InteractiveChartCard(
                    points = points,
                    line = render.chartType == ReportChart.LINE,
                    summary = render.summary,
                    onViewMovements = onView,
                )
            }

            // resumen textual accesible del reporte; siempre disponible como alternativa a la grafica
            Text(
                text = render.summary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.REPORT_SUMMARY),
            )

            // bajo privacidad de saldos no se muestran los montos de la tabla
            if (hidden) {
                Text(
                    text = stringResource(R.string.balances_hidden_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (showTable) {
                ReportTable(render)
            }

            Button(onClick = onExport, modifier = Modifier.fillMaxWidth().testTag(TestTags.EXPORT_REPORT_BUTTON)) {
                Text(stringResource(R.string.export_csv))
            }
        }
    }
}

// selector del modo de vista del reporte: grafica, lista o ambas
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeToggle(viewMode: ReportViewMode, onViewMode: (ReportViewMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag(TestTags.REPORT_VIEW_TOGGLE),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = viewMode == ReportViewMode.CHART,
            onClick = { onViewMode(ReportViewMode.CHART) },
            label = { Text(stringResource(R.string.report_view_chart)) },
            modifier = Modifier.testTag(TestTags.REPORT_CHART_MODE),
        )
        FilterChip(
            selected = viewMode == ReportViewMode.LIST,
            onClick = { onViewMode(ReportViewMode.LIST) },
            label = { Text(stringResource(R.string.report_view_list)) },
            modifier = Modifier.testTag(TestTags.REPORT_LIST_MODE),
        )
        FilterChip(
            selected = viewMode == ReportViewMode.BOTH,
            onClick = { onViewMode(ReportViewMode.BOTH) },
            label = { Text(stringResource(R.string.report_view_both)) },
            modifier = Modifier.testTag(TestTags.REPORT_BOTH_MODE),
        )
    }
}

@Composable
private fun CustomRangePickers(state: ReportUiState, onCustomRange: (LocalDate, LocalDate) -> Unit) {
    val start = state.customStart ?: LocalDate.now().withDayOfMonth(1)
    val end = state.customEnd ?: LocalDate.now()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.kratt.finanzas.presentation.components.DateField(
            date = start, onDateSelected = { onCustomRange(it, end) },
            label = stringResource(R.string.from_label), tag = "report_from", modifier = Modifier.weight(1f),
        )
        com.kratt.finanzas.presentation.components.DateField(
            date = end, onDateSelected = { onCustomRange(start, it) },
            label = stringResource(R.string.to_label), tag = "report_to", modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ReportTable(render: ReportRender) {
    if (render.tableHeader.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag(TestTags.REPORT_TABLE)) {
        Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            render.tableHeader.forEach { cell ->
                Text(cell, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(120.dp))
            }
        }
        HorizontalDivider()
        if (render.tableRows.isEmpty()) {
            Text(stringResource(R.string.no_results), modifier = Modifier.padding(vertical = 8.dp))
        }
        render.tableRows.forEach { row ->
            Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { cell ->
                    Text(cell, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(120.dp))
                }
            }
        }
    }
}
