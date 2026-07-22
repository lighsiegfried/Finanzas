package com.kratt.finanzas.presentation.lock

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
class LockScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun setContent(
        state: LockUiState,
        onUnlockClick: () -> Unit = {},
        onContinueClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            MisFinanzasTheme {
                LockScreen(
                    state = state,
                    onUnlockClick = onUnlockClick,
                    onContinueClick = onContinueClick,
                )
            }
        }
    }

    @Test
    fun lockScreen_showsUnlockButton_andNoFinancialData() {
        setContent(LockUiState())

        composeRule.onNodeWithTag(TestTags.UNLOCK_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.unlock_action)).assertIsDisplayed()
        // la pantalla de bloqueo jamas muestra montos ni secciones financieras
        composeRule.onAllNodesWithText("Q0.00").assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.income_label)).assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.expense_label)).assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.monthly_balance_label))
            .assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.recent_transactions_title))
            .assertCountEquals(0)
    }

    @Test
    fun failedState_showsSpanishFailureMessage() {
        setContent(LockUiState(LockScreenStatus.FAILED, R.string.auth_failed_message))

        composeRule.onNodeWithText(context.getString(R.string.auth_failed_message))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.UNLOCK_BUTTON).assertIsDisplayed()
    }

    @Test
    fun unavailableState_showsGuidanceAndContinue() {
        setContent(LockUiState(LockScreenStatus.UNAVAILABLE, R.string.device_lock_unavailable))

        composeRule.onNodeWithText(context.getString(R.string.device_lock_unavailable))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.CONTINUE_BUTTON).assertIsDisplayed()
        composeRule.onAllNodesWithTag(TestTags.UNLOCK_BUTTON).assertCountEquals(0)
    }

    @Test
    fun authenticatingState_disablesUnlockButton() {
        setContent(LockUiState(LockScreenStatus.AUTHENTICATING))

        composeRule.onNodeWithTag(TestTags.UNLOCK_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun unlockButton_invokesCallback() {
        var clicked = false
        setContent(LockUiState(), onUnlockClick = { clicked = true })

        composeRule.onNodeWithTag(TestTags.UNLOCK_BUTTON).performClick()
        assertTrue(clicked)
    }
}
