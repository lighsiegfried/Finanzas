package com.kratt.finanzas.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.domain.model.DashboardModule
import com.kratt.finanzas.domain.model.Density as DisplayDensity
import com.kratt.finanzas.domain.model.QuickAction
import com.kratt.finanzas.domain.model.ReportViewMode
import com.kratt.finanzas.domain.model.ThemeMode
import com.kratt.finanzas.presentation.charts.ChartPoint
import com.kratt.finanzas.presentation.charts.InteractiveChartCard
import com.kratt.finanzas.presentation.common.LocalBalancesHidden
import com.kratt.finanzas.presentation.common.LocalListDensity
import com.kratt.finanzas.presentation.report.ReportChart
import com.kratt.finanzas.presentation.report.ReportRender
import com.kratt.finanzas.presentation.report.ReportScreen
import com.kratt.finanzas.presentation.report.ReportType
import com.kratt.finanzas.presentation.report.ReportUiState
import com.kratt.finanzas.presentation.summary.AccountBalanceItem
import com.kratt.finanzas.presentation.summary.CategoryTotalItem
import com.kratt.finanzas.presentation.summary.SummaryScreen
import com.kratt.finanzas.presentation.summary.SummaryUiState
import com.kratt.finanzas.presentation.theme.MisFinanzasTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// captura evidencia visual de las pantallas de la fase 5a usando datos de demostracion, nunca datos reales
@RunWith(AndroidJUnit4::class)
class Phase5aEvidenceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val outDir: File by lazy {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        File(ctx.getExternalFilesDir(null), "phase5a").apply { mkdirs() }
    }

    private fun demoSummary() = SummaryUiState(
        isLoading = false, monthLabel = "Julio de 2026",
        incomeCents = 800_000, expenseCents = 525_000, balanceCents = 275_000,
        quickActions = QuickAction.DEFAULTS,
        accountBalances = listOf(AccountBalanceItem("Efectivo", 120_000), AccountBalanceItem("Banco", 340_000)),
        expenseCategories = listOf(CategoryTotalItem("Comida", 210_000), CategoryTotalItem("Transporte", 90_000)),
    )

    private fun demoRender() = ReportRender(
        chartType = ReportChart.COLUMN,
        chartValues = listOf(2100.0, 900.0, 450.0),
        chartLabels = listOf("Comida", "Transporte", "Servicios"),
        chartAmountsCents = listOf(210_000, 90_000, 45_000),
        summary = "Gastos por categoría: Comida Q2,100.00, Transporte Q900.00, Servicios Q450.00.",
        tableHeader = listOf("Categoría", "Total", "%"),
        tableRows = listOf(listOf("Comida", "Q2,100.00", "61%"), listOf("Transporte", "Q900.00", "26%")),
    )

    // pixelcopy a veces falla por tiempo; se reintenta unas veces antes de rendirse
    private fun capture(name: String) {
        var last: Throwable? = null
        repeat(4) {
            try {
                composeRule.waitForIdle()
                val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
                File(outDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                return
            } catch (t: Throwable) {
                last = t
                Thread.sleep(300)
            }
        }
        throw last ?: IllegalStateException("captura fallida")
    }

    @Composable
    private fun Themed(dark: Boolean, content: @Composable () -> Unit) {
        MisFinanzasTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
            Surface(modifier = Modifier.fillMaxSize()) { content() }
        }
    }

    @Test
    fun a_lightThemeDashboard() {
        composeRule.setContent {
            Themed(dark = false) {
                SummaryScreen(
                    state = demoSummary(),
                    modules = listOf(DashboardModule.QUICK_ACTIONS, DashboardModule.ACCOUNT_BALANCES, DashboardModule.EXPENSE_CATEGORIES),
                    onAddTransactionClick = {}, onSettingsClick = {},
                )
            }
        }
        capture("light-theme.png")
        capture("dashboard-default.png")
        capture("quick-actions.png")
    }

    @Test
    fun b_darkThemeDashboard() {
        composeRule.setContent {
            Themed(dark = true) {
                SummaryScreen(
                    state = demoSummary(),
                    modules = listOf(DashboardModule.QUICK_ACTIONS, DashboardModule.ACCOUNT_BALANCES, DashboardModule.EXPENSE_CATEGORIES),
                    onAddTransactionClick = {}, onSettingsClick = {},
                )
            }
        }
        capture("dark-theme.png")
    }

    @Test
    fun c_hiddenBalances() {
        composeRule.setContent {
            Themed(dark = false) {
                CompositionLocalProvider(LocalBalancesHidden provides true) {
                    SummaryScreen(
                        state = demoSummary(), balancesHidden = true,
                        modules = listOf(DashboardModule.ACCOUNT_BALANCES),
                        onAddTransactionClick = {}, onSettingsClick = {},
                    )
                }
            }
        }
        capture("hidden-balances.png")
    }

    @Test
    fun d_compactDensity() {
        composeRule.setContent {
            Themed(dark = false) {
                CompositionLocalProvider(LocalListDensity provides DisplayDensity.COMPACT) {
                    SummaryScreen(
                        state = demoSummary(),
                        modules = listOf(DashboardModule.ACCOUNT_BALANCES, DashboardModule.EXPENSE_CATEGORIES),
                        onAddTransactionClick = {}, onSettingsClick = {},
                    )
                }
            }
        }
        capture("compact-mode.png")
    }

    @Test
    fun e_largeFont() {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = base.density, fontScale = 1.5f)) {
                Themed(dark = false) {
                    SummaryScreen(state = demoSummary(), onAddTransactionClick = {}, onSettingsClick = {})
                }
            }
        }
        capture("large-font-validation.png")
    }

    @Test
    fun f_interactiveChart() {
        composeRule.setContent {
            Themed(dark = false) {
                InteractiveChartCard(
                    points = listOf(ChartPoint("Comida", 210_000), ChartPoint("Transporte", 90_000), ChartPoint("Servicios", 45_000)),
                    line = false, summary = "resumen", onViewMovements = {},
                )
            }
        }
        // toca el primer punto para mostrar la tarjeta de detalle antes de capturar
        composeRule.onNode(hasTestTag("chart_point_0")).performClick()
        capture("interactive-chart.png")
    }

    @Test
    fun g_reportChartMode() {
        composeRule.setContent {
            Themed(dark = false) {
                val snackbar = remember { SnackbarHostState() }
                ReportScreen(
                    type = ReportType.BY_CATEGORY, state = ReportUiState(isLoading = false, render = demoRender()),
                    viewMode = ReportViewMode.CHART, snackbarHostState = snackbar,
                    onBack = {}, onPeriod = {}, onCustomRange = { _, _ -> }, onViewMode = {}, onOpenMovements = {}, onExport = {},
                )
            }
        }
        capture("report-chart-mode.png")
    }

    @Test
    fun h_reportListMode() {
        composeRule.setContent {
            Themed(dark = false) {
                val snackbar = remember { SnackbarHostState() }
                ReportScreen(
                    type = ReportType.BY_CATEGORY, state = ReportUiState(isLoading = false, render = demoRender()),
                    viewMode = ReportViewMode.LIST, snackbarHostState = snackbar,
                    onBack = {}, onPeriod = {}, onCustomRange = { _, _ -> }, onViewMode = {}, onOpenMovements = {}, onExport = {},
                )
            }
        }
        capture("report-list-mode.png")
    }
}
