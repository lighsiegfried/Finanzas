package com.kratt.finanzas.presentation.backup

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
class BackupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun setContent(
        state: BackupUiState,
        onCreateClick: () -> Unit = {},
        onRestoreClick: () -> Unit = {},
        onCreateContinue: () -> Unit = {},
    ) {
        composeRule.setContent {
            MisFinanzasTheme {
                BackupScreen(
                    state = state,
                    onBack = {},
                    onCreateClick = onCreateClick,
                    onRestoreClick = onRestoreClick,
                    onPasswordChange = {},
                    onConfirmChange = {},
                    onToggleShow = {},
                    onToggleIncludeAttachments = {},
                    onCreateContinue = onCreateContinue,
                    onExportConfirm = {},
                    onRestorePasswordSubmit = {},
                    onRestoreConfirm = {},
                    onCancel = {},
                    onDismiss = {},
                )
            }
        }
    }

    @Test
    fun idleShowsButtonsAndNeverLabel() {
        setContent(BackupUiState(stage = BackupStage.IDLE, hasBackup = false))
        composeRule.onNodeWithTag(TestTags.BACKUP_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.CREATE_BACKUP_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.RESTORE_BACKUP_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.backup_never)).assertIsDisplayed()
    }

    @Test
    fun createButtonInvokesCallback() {
        var clicked = false
        setContent(BackupUiState(stage = BackupStage.IDLE), onCreateClick = { clicked = true })
        composeRule.onNodeWithTag(TestTags.CREATE_BACKUP_BUTTON).performClick()
        assertTrue(clicked)
    }

    @Test
    fun restoreButtonInvokesCallback() {
        var clicked = false
        setContent(BackupUiState(stage = BackupStage.IDLE), onRestoreClick = { clicked = true })
        composeRule.onNodeWithTag(TestTags.RESTORE_BACKUP_BUTTON).performClick()
        assertTrue(clicked)
    }

    @Test
    fun createPasswordDialogShowsFields() {
        setContent(BackupUiState(stage = BackupStage.CREATE_PASSWORD))
        composeRule.onNodeWithTag(TestTags.PASSWORD_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.CONFIRM_PASSWORD_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.SHOW_PASSWORD_TOGGLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.PASSWORD_CONTINUE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun passwordErrorIsDisplayed() {
        setContent(BackupUiState(stage = BackupStage.CREATE_PASSWORD, passwordErrorRes = R.string.password_too_short))
        composeRule.onNodeWithText(context.getString(R.string.password_too_short)).assertIsDisplayed()
    }

    @Test
    fun continueButtonInvokesCallback() {
        var clicked = false
        setContent(BackupUiState(stage = BackupStage.CREATE_PASSWORD), onCreateContinue = { clicked = true })
        composeRule.onNodeWithTag(TestTags.PASSWORD_CONTINUE_BUTTON).performClick()
        assertTrue(clicked)
    }

    @Test
    fun restoreConfirmShowsWarningAndButton() {
        setContent(BackupUiState(stage = BackupStage.RESTORE_CONFIRM))
        composeRule.onNodeWithText(context.getString(R.string.restore_confirm_message)).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.RESTORE_CONFIRM_BUTTON).assertIsDisplayed()
    }

    @Test
    fun successShowsCreatedMessage() {
        setContent(
            BackupUiState(
                stage = BackupStage.SUCCESS,
                resultTitleRes = R.string.backup_created_title,
                resultMessageRes = R.string.backup_created_message,
            ),
        )
        composeRule.onNodeWithText(context.getString(R.string.backup_created_message)).assertIsDisplayed()
    }
}
