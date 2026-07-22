package com.kratt.finanzas.presentation.summary

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.presentation.theme.MisFinanzasTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SummaryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun setContent(
        state: SummaryUiState,
        onAddTransactionClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            MisFinanzasTheme {
                SummaryScreen(
                    state = state,
                    onAddTransactionClick = onAddTransactionClick,
                    onSettingsClick = {},
                )
            }
        }
    }

    @Test
    fun summaryScreen_showsSpanishLabels() {
        setContent(SummaryUiState(isLoading = false, monthLabel = "Julio de 2026"))

        composeRule.onNodeWithText(context.getString(R.string.summary_title)).assertIsDisplayed()
        composeRule.onNodeWithText("Julio de 2026").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.income_label)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.expense_label)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.monthly_balance_label)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.recent_transactions_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.summary_empty_state)).assertIsDisplayed()
        // el boton principal se valida por tag porque su texto vive en un contenedor animado
        composeRule.onNodeWithTag(TestTags.ADD_TRANSACTION_BUTTON).assertIsDisplayed()
    }

    @Test
    fun summaryScreen_zeroState_showsThreeZeroAmounts() {
        setContent(SummaryUiState(isLoading = false, monthLabel = "Julio de 2026"))

        composeRule.onAllNodesWithText("Q0.00").assertCountEquals(3)
    }

    @Test
    fun summaryScreen_addButton_invokesCallback() {
        var clicked = false
        setContent(
            SummaryUiState(isLoading = false, monthLabel = "Julio de 2026"),
            onAddTransactionClick = { clicked = true },
        )

        composeRule.onNodeWithTag(TestTags.ADD_TRANSACTION_BUTTON).assertIsDisplayed().performClick()
        assertTrue(clicked)
    }
}
