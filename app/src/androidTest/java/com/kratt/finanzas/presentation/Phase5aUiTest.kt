package com.kratt.finanzas.presentation

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.R
import com.kratt.finanzas.common.CurrencyFormatter
import com.kratt.finanzas.domain.model.DashboardModule
import com.kratt.finanzas.domain.usecase.DueWhen
import com.kratt.finanzas.presentation.common.AMOUNT_MASK
import com.kratt.finanzas.presentation.common.LocalBalancesHidden
import com.kratt.finanzas.presentation.summary.SummaryUiState
import com.kratt.finanzas.presentation.summary.SummaryScreen
import com.kratt.finanzas.presentation.summary.UpcomingItem
import com.kratt.finanzas.presentation.theme.MisFinanzasTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase5aUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun state() = SummaryUiState(
        isLoading = false, monthLabel = "Julio de 2026",
        incomeCents = 800_000, expenseCents = 525_000, balanceCents = 275_000,
    )

    private fun render(hidden: Boolean, uiState: SummaryUiState = state(), modules: List<DashboardModule> = DashboardModule.DEFAULT_ORDER) {
        composeRule.setContent {
            MisFinanzasTheme {
                CompositionLocalProvider(LocalBalancesHidden provides hidden) {
                    SummaryScreen(state = uiState, modules = modules, balancesHidden = hidden, onAddTransactionClick = {}, onSettingsClick = {})
                }
            }
        }
    }

    @Test
    fun amountsVisible_whenPrivacyOff() {
        render(hidden = false)
        composeRule.onNodeWithText(CurrencyFormatter.format(800_000)).assertIsDisplayed()
        composeRule.onAllNodesWithText(AMOUNT_MASK).assertCountEquals(0)
    }

    @Test
    fun amountsMasked_whenPrivacyOn_andRealValueAbsent() {
        render(hidden = true)
        // ingresos, gastos y balance quedan enmascarados
        composeRule.onAllNodesWithText(AMOUNT_MASK).assertCountEquals(3)
        // el valor real no aparece en el arbol ni en la semantica
        composeRule.onAllNodesWithText(CurrencyFormatter.format(800_000)).assertCountEquals(0)
    }

    @Test
    fun upcomingModule_hidden_whenNotInDashboardOrder() {
        val withUpcoming = state().copy(
            committedCents = 20_000,
            upcoming = listOf(UpcomingItem(LocalDate.of(2026, 7, 20), 20_000, DueWhen.LATER)),
        )
        // solo el modulo de movimientos recientes, sin proximos pagos
        render(hidden = false, uiState = withUpcoming, modules = listOf(DashboardModule.RECENT))
        composeRule.onAllNodesWithText(context.getString(R.string.upcoming_payments_title)).assertCountEquals(0)
    }

    @Test
    fun upcomingModule_shown_whenInDashboardOrder() {
        val withUpcoming = state().copy(
            committedCents = 20_000,
            upcoming = listOf(UpcomingItem(LocalDate.of(2026, 7, 20), 20_000, DueWhen.LATER)),
        )
        render(hidden = false, uiState = withUpcoming, modules = listOf(DashboardModule.UPCOMING, DashboardModule.RECENT))
        composeRule.onNodeWithText(context.getString(R.string.upcoming_payments_title)).assertIsDisplayed()
    }
}
