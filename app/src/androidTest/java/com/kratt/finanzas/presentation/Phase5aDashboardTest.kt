package com.kratt.finanzas.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.common.CurrencyFormatter
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.DashboardModule
import com.kratt.finanzas.domain.model.QuickAction
import com.kratt.finanzas.presentation.common.LocalListDensity
import com.kratt.finanzas.presentation.summary.AccountBalanceItem
import com.kratt.finanzas.presentation.summary.SummaryScreen
import com.kratt.finanzas.presentation.summary.SummaryUiState
import com.kratt.finanzas.presentation.theme.MisFinanzasTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase5aDashboardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quickActionsModule_rendersActions_andClickNavigates() {
        var clicked: QuickAction? = null
        composeRule.setContent {
            MisFinanzasTheme {
                SummaryScreen(
                    state = SummaryUiState(isLoading = false, quickActions = listOf(QuickAction.ADD_EXPENSE, QuickAction.TRANSFER)),
                    modules = listOf(DashboardModule.QUICK_ACTIONS),
                    onAddTransactionClick = {}, onSettingsClick = {}, onQuickAction = { clicked = it },
                )
            }
        }
        composeRule.onNodeWithTag(TestTags.DASHBOARD_QUICK_ACTIONS).assertIsDisplayed()
        composeRule.onNodeWithTag("${TestTags.DASHBOARD_QUICK_ACTION}_${QuickAction.ADD_EXPENSE.name}").performClick()
        assertEquals(QuickAction.ADD_EXPENSE, clicked)
    }

    @Test
    fun accountBalancesModule_rendersData() {
        composeRule.setContent {
            MisFinanzasTheme {
                SummaryScreen(
                    state = SummaryUiState(
                        isLoading = false,
                        accountBalances = listOf(AccountBalanceItem("Efectivo", 50_000)),
                    ),
                    modules = listOf(DashboardModule.ACCOUNT_BALANCES),
                    onAddTransactionClick = {}, onSettingsClick = {},
                )
            }
        }
        composeRule.onNodeWithTag(TestTags.MODULE_ACCOUNT_BALANCES).assertIsDisplayed()
        composeRule.onNodeWithText("Efectivo").assertIsDisplayed()
        composeRule.onNodeWithText(CurrencyFormatter.format(50_000)).assertIsDisplayed()
    }

    @Test
    fun compactDensity_rendersWithoutCrash() {
        composeRule.setContent {
            MisFinanzasTheme {
                CompositionLocalProvider(LocalListDensity provides com.kratt.finanzas.domain.model.Density.COMPACT) {
                    SummaryScreen(
                        state = SummaryUiState(isLoading = false, monthLabel = "Julio de 2026"),
                        onAddTransactionClick = {}, onSettingsClick = {},
                    )
                }
            }
        }
        composeRule.onNodeWithTag(TestTags.SUMMARY_TITLE).assertIsDisplayed()
    }

    @Test
    fun largeFontScale_keepsScreenUsable() {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = base.density, fontScale = 1.5f)) {
                MisFinanzasTheme {
                    SummaryScreen(
                        state = SummaryUiState(isLoading = false, monthLabel = "Julio de 2026", incomeCents = 800_000),
                        onAddTransactionClick = {}, onSettingsClick = {},
                    )
                }
            }
        }
        composeRule.onNodeWithTag(TestTags.SUMMARY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.SUMMARY_INCOME_CARD).assertIsDisplayed()
    }
}
