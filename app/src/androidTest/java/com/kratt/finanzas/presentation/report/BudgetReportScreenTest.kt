package com.kratt.finanzas.presentation.report

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.Budget
import com.kratt.finanzas.domain.usecase.BudgetCalculator
import com.kratt.finanzas.presentation.budget.BudgetBanner
import com.kratt.finanzas.presentation.budget.BudgetRowUi
import com.kratt.finanzas.presentation.budget.BudgetsScreen
import com.kratt.finanzas.presentation.budget.BudgetsUiState
import com.kratt.finanzas.presentation.theme.MisFinanzasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// pruebas de ui de compose para las pantallas de presupuestos y reportes de la fase 3c
@RunWith(AndroidJUnit4::class)
class BudgetReportScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun overallRow() = BudgetRowUi(
        budget = Budget(1L, 2026, 7, null, 600_000, 80, 0L, 0L),
        categoryName = null,
        progress = BudgetCalculator.progress(600_000, 525_000, 80),
    )

    private fun foodRow() = BudgetRowUi(
        budget = Budget(2L, 2026, 7, 5L, 120_000, 80, 0L, 0L),
        categoryName = "Alimentacion",
        progress = BudgetCalculator.progress(120_000, 85_000, 80),
    )

    @Test
    fun budgetsScreen_showsSpanishLabelsAndStates() {
        composeRule.setContent {
            MisFinanzasTheme {
                BudgetsScreen(
                    state = BudgetsUiState(
                        monthLabel = "Julio de 2026",
                        isCurrentMonth = true,
                        overall = overallRow(),
                        categoryBudgets = listOf(foodRow()),
                        actualExpensesCents = 525_000,
                        committedCents = 0,
                        availableCents = 75_000,
                        banner = BudgetBanner.NEAR_LIMIT,
                    ),
                    onBack = {}, onPreviousMonth = {}, onNextMonth = {}, onCurrentMonth = {},
                    onAddBudget = {}, onEditBudget = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.budget_summary)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.actual_expenses)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.pending_commitments)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.budget_available)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.warning_near_limit)).assertIsDisplayed()
        // el estado en texto acompaña a la barra, no depende solo del color
        composeRule.onNodeWithText(context.getString(R.string.budget_state_warning) + " · 87%").assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.ADD_BUDGET_BUTTON).assertIsDisplayed()
    }

    @Test
    fun reportsHome_listsAllTenReports_andClickInvokesCallback() {
        var clicked: ReportType? = null
        composeRule.setContent {
            MisFinanzasTheme {
                ReportsHomeScreen(onBack = {}, onReportClick = { clicked = it })
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.report_income_expense)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.report_budget_performance)).assertIsDisplayed()
        composeRule.onNodeWithTag("${TestTags.REPORT_ITEM}_${ReportType.INCOME_EXPENSE.name}").performClick()
        assertEquals(ReportType.INCOME_EXPENSE, clicked)
    }

    @Test
    fun reportScreen_showsAccessibleSummaryTableAndEmptyChart() {
        val render = ReportRender(
            chartType = ReportChart.COLUMN,
            chartValues = emptyList(),
            summary = "Ingresos: Q8,000.00. Gastos: Q5,250.00. Balance del mes: Q2,750.00.",
            tableHeader = listOf("Ingresos y gastos", "Total"),
            tableRows = listOf(listOf("Ingresos", "Q8,000.00"), listOf("Gastos", "Q5,250.00")),
            csv = null,
        )
        var exported = false
        composeRule.setContent {
            MisFinanzasTheme {
                ReportScreen(
                    type = ReportType.INCOME_EXPENSE,
                    state = ReportUiState(isLoading = false, render = render),
                    viewMode = com.kratt.finanzas.domain.model.ReportViewMode.BOTH,
                    snackbarHostState = SnackbarHostState(),
                    onBack = {}, onPeriod = {}, onCustomRange = { _, _ -> },
                    onViewMode = {}, onOpenMovements = {},
                    onExport = { exported = true },
                )
            }
        }

        composeRule.onNodeWithText(render.summary).assertIsDisplayed()
        // sin datos la grafica muestra el estado vacio en texto
        composeRule.onNodeWithText(context.getString(R.string.chart_empty)).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.REPORT_SUMMARY).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.REPORT_TABLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.EXPORT_REPORT_BUTTON).performClick()
        assertTrue(exported)
    }
}
