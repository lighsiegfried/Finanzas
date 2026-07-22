package com.kratt.finanzas.presentation.report

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kratt.finanzas.R

// reportes con layout adaptable: en pantallas expandidas muestra lista y detalle a la vez
// reusa las mismas pantallas y viewmodels; no duplica logica de reportes
@Composable
fun AdaptiveReportsScreen(
    expanded: Boolean,
    onBack: () -> Unit,
    onReportClick: (ReportType) -> Unit,
    onOpenMovements: (ReportChartFilter) -> Unit,
) {
    if (!expanded) {
        ReportsHomeScreen(onBack = onBack, onReportClick = onReportClick)
        return
    }
    // en dos paneles el panel izquierdo elige el reporte y el derecho lo muestra
    var selected by rememberSaveable { mutableStateOf(ReportType.INCOME_EXPENSE.name) }
    val navTitle = stringResource(R.string.pane_navigation)
    val detailTitle = stringResource(R.string.pane_detail)
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.width(340.dp).fillMaxHeight().semantics { paneTitle = navTitle },
        ) {
            ReportsHomeScreen(onBack = onBack, onReportClick = { selected = it.name })
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight().semantics { paneTitle = detailTitle },
        ) {
            ReportRoute(type = ReportType.valueOf(selected), onBack = onBack, onOpenMovements = onOpenMovements)
        }
    }
}
