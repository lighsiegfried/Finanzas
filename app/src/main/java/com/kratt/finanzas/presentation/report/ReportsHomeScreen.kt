package com.kratt.finanzas.presentation.report

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags

// etiqueta en espanol de cada reporte
@StringRes
fun ReportType.labelRes(): Int = when (this) {
    ReportType.INCOME_EXPENSE -> R.string.report_income_expense
    ReportType.BY_CATEGORY -> R.string.report_by_category
    ReportType.BY_ACCOUNT -> R.string.report_by_account
    ReportType.TREND -> R.string.report_trend
    ReportType.ACCOUNT_SUMMARY -> R.string.report_account_summary
    ReportType.CREDIT_DEBT -> R.string.report_credit_debt
    ReportType.INSTALLMENT_COMMITMENTS -> R.string.report_installment_commitments
    ReportType.RECURRING_COMMITMENTS -> R.string.report_recurring
    ReportType.BUDGET_PERFORMANCE -> R.string.report_budget_performance
    ReportType.COMPARISON -> R.string.report_comparison
    ReportType.SAVINGS_PROGRESS -> R.string.report_savings_progress
    ReportType.CONTRIBUTIONS_BY_MONTH -> R.string.report_contributions_by_month
    ReportType.ACTIVE_GOALS -> R.string.report_active_goals
    ReportType.COMPLETED_GOALS -> R.string.report_completed_goals
    ReportType.PLANNED_PURCHASES_REPORT -> R.string.report_planned_purchases
    ReportType.PLANNED_BY_PRIORITY -> R.string.report_planned_by_priority
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsHomeScreen(onBack: () -> Unit, onReportClick: (ReportType) -> Unit) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.REPORTS_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.reports_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ReportType.entries) { report ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onReportClick(report) }.testTag("${TestTags.REPORT_ITEM}_${report.name}"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(report.labelRes()), style = MaterialTheme.typography.bodyLarge)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
            item { Column(Modifier.padding(4.dp)) {} }
        }
    }
}
