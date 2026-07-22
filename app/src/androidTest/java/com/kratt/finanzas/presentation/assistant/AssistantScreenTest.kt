package com.kratt.finanzas.presentation.assistant

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.assistant.AssistantAction
import com.kratt.finanzas.domain.assistant.AssistantAnswer
import com.kratt.finanzas.domain.assistant.AssistantMode
import com.kratt.finanzas.domain.assistant.AssistantStatus
import com.kratt.finanzas.domain.assistant.GenerativeAvailability
import com.kratt.finanzas.domain.assistant.PeriodLabel
import com.kratt.finanzas.domain.model.LabeledTotal
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val status = AssistantStatus(AssistantMode.DETERMINISTIC_LOCAL, GenerativeAvailability.UNSUPPORTED_DEVICE)

    private fun setScreen(
        state: AssistantUiState,
        onSend: () -> Unit = {},
        onClear: () -> Unit = {},
        onAction: (AssistantAction) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                AssistantScreen(
                    state = state,
                    onBack = {},
                    onInputChange = {},
                    onSend = onSend,
                    onSuggestion = {},
                    onCancel = {},
                    onClear = onClear,
                    onAction = onAction,
                )
            }
        }
    }

    @Test
    fun home_shows_suggestions_and_privacy_note() {
        setScreen(AssistantUiState(status = status))
        composeRule.onNodeWithTag(TestTags.ASSISTANT_SUGGESTIONS).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.assistant_privacy_note)).assertIsDisplayed()
    }

    @Test
    fun how_calculated_expands_for_summary_answer() {
        val answer = AssistantAnswer.MonthlySummaryAnswer(PeriodLabel.Month(2026, 7), 800_00, 525_00, 275_00)
        setScreen(AssistantUiState(messages = listOf(AssistantChatMessage.Bot(answer)), status = status))
        composeRule.onNodeWithText("Periodo", substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag(TestTags.ASSISTANT_HOW_CALCULATED).performClick()
        composeRule.onNodeWithText("Periodo", substring = true).assertIsDisplayed()
    }

    @Test
    fun view_movements_action_is_invoked() {
        var invoked = false
        val answer = AssistantAnswer.ExpensesByCategoryAnswer(
            period = PeriodLabel.Month(2026, 7),
            items = listOf(LabeledTotal(1, "Alimentación", 850_00, 6)),
            action = AssistantAction.ViewMovements(categoryId = 1),
        )
        setScreen(
            AssistantUiState(messages = listOf(AssistantChatMessage.Bot(answer)), status = status),
            onAction = { invoked = true },
        )
        composeRule.onNodeWithTag(TestTags.ASSISTANT_ACTION).performClick()
        assertTrue(invoked)
    }

    @Test
    fun clear_button_invokes_callback() {
        var cleared = false
        val answer = AssistantAnswer.HelpAnswer
        setScreen(AssistantUiState(messages = listOf(AssistantChatMessage.Bot(answer)), status = status), onClear = { cleared = true })
        composeRule.onNodeWithTag(TestTags.ASSISTANT_CLEAR).performClick()
        assertTrue(cleared)
    }

    @Test
    fun send_button_invokes_callback_when_input_present() {
        var sent = false
        setScreen(AssistantUiState(input = "hola", status = status), onSend = { sent = true })
        composeRule.onNodeWithTag(TestTags.ASSISTANT_SEND).performClick()
        assertTrue(sent)
    }
}
