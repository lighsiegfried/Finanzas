package com.kratt.finanzas.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.ReportViewMode
import com.kratt.finanzas.presentation.report.ReportChart
import com.kratt.finanzas.presentation.report.ReportRender
import com.kratt.finanzas.presentation.report.ReportScreen
import com.kratt.finanzas.presentation.report.ReportType
import com.kratt.finanzas.presentation.report.ReportUiState
import com.kratt.finanzas.presentation.theme.MisFinanzasTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase5aReportModesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val render = ReportRender(
        chartType = ReportChart.COLUMN,
        chartValues = listOf(1.0, 2.0),
        chartLabels = listOf("Comida", "Transporte"),
        chartAmountsCents = listOf(100, 200),
        summary = "resumen del reporte",
        tableHeader = listOf("Categoría", "Total"),
        tableRows = listOf(listOf("Comida", "Q1.00")),
    )

    private fun render(mode: ReportViewMode) {
        composeRule.setContent {
            MisFinanzasTheme {
                val snackbar = remember { SnackbarHostState() }
                ReportScreen(
                    type = ReportType.BY_CATEGORY,
                    state = ReportUiState(isLoading = false, render = render),
                    viewMode = mode,
                    snackbarHostState = snackbar,
                    onBack = {}, onPeriod = {}, onCustomRange = { _, _ -> },
                    onViewMode = {}, onOpenMovements = {}, onExport = {},
                )
            }
        }
    }

    @Test
    fun chartMode_showsChart_hidesTable() {
        render(ReportViewMode.CHART)
        composeRule.onNodeWithTag(TestTags.CHART_INTERACTIVE).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.REPORT_TABLE).assertDoesNotExist()
    }

    @Test
    fun listMode_showsTable_hidesChart() {
        render(ReportViewMode.LIST)
        composeRule.onNodeWithTag(TestTags.REPORT_TABLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.CHART_INTERACTIVE).assertDoesNotExist()
    }

    @Test
    fun bothMode_showsChartAndTable() {
        render(ReportViewMode.BOTH)
        composeRule.onNodeWithTag(TestTags.CHART_INTERACTIVE).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.REPORT_TABLE).assertIsDisplayed()
    }

    @Test
    fun periodSelectorAndViewToggle_arePresent() {
        render(ReportViewMode.BOTH)
        composeRule.onNodeWithTag(TestTags.PERIOD_SELECTOR).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.REPORT_VIEW_TOGGLE).assertIsDisplayed()
    }
}
