package com.kratt.finanzas.presentation.security

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.LockTimeout
import com.kratt.finanzas.presentation.theme.MisFinanzasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecuritySettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun setContent(
        state: SecuritySettingsUiState,
        onToggleLock: (Boolean) -> Unit = {},
        onTimeoutSelected: (LockTimeout) -> Unit = {},
    ) {
        composeRule.setContent {
            MisFinanzasTheme {
                SecuritySettingsScreen(
                    state = state,
                    onToggleLock = onToggleLock,
                    onTimeoutSelected = onTimeoutSelected,
                    onEnableErrorShown = {},
                    onBack = {},
                    onOpenBackup = {},
                )
            }
        }
    }

    // por defecto el timeout es SESSION (viene de SecuritySettingsUiState)
    private fun availableState() = SecuritySettingsUiState(
        isLoading = false,
        lockEnabled = true,
        authAvailable = true,
    )

    @Test
    fun securitySettings_showOnlyTheTwoNewSpanishOptions() {
        setContent(availableState())

        listOf(
            R.string.security_title,
            R.string.app_lock_section,
            R.string.use_biometric_or_device_lock,
            R.string.auto_lock_section,
            R.string.timeout_session,
            R.string.timeout_session_supporting,
            R.string.timeout_ten_minutes,
            R.string.timeout_ten_minutes_supporting,
        ).forEach { res ->
            composeRule.onNodeWithText(context.getString(res)).assertExists()
        }
    }

    @Test
    fun obsoleteTimeoutLabels_areAbsent() {
        setContent(availableState())

        composeRule.onAllNodesWithText("Inmediatamente").assertCountEquals(0)
        composeRule.onAllNodesWithText("Después de 30 segundos").assertCountEquals(0)
        composeRule.onAllNodesWithText("Después de 1 minuto").assertCountEquals(0)
        composeRule.onAllNodesWithText("Después de 5 minutos").assertCountEquals(0)
    }

    @Test
    fun session_isSelectedByDefault() {
        setContent(availableState())

        composeRule.onNodeWithTag(TestTags.TIMEOUT_SESSION).assertIsSelected()
        composeRule.onNodeWithTag(TestTags.TIMEOUT_TEN_MINUTES).assertIsNotSelected()
    }

    @Test
    fun unavailableAuth_disablesSwitch_andShowsGuidance() {
        setContent(availableState().copy(authAvailable = false, lockEnabled = false))

        composeRule.onNodeWithTag(TestTags.LOCK_SWITCH).assertIsNotEnabled()
        composeRule.onNodeWithText(context.getString(R.string.device_lock_unavailable))
            .assertIsDisplayed()
    }

    @Test
    fun togglingSwitchOn_requestsEnable() {
        var requested: Boolean? = null
        setContent(availableState().copy(lockEnabled = false), onToggleLock = { requested = it })

        composeRule.onNodeWithTag(TestTags.LOCK_SWITCH).performClick()
        assertEquals(true, requested)
    }

    @Test
    fun tenMinutesOption_selectionInvokesCallback() {
        var selected: LockTimeout? = null
        setContent(availableState(), onTimeoutSelected = { selected = it })

        composeRule.onNodeWithTag(TestTags.TIMEOUT_TEN_MINUTES).performClick()
        assertEquals(LockTimeout.TEN_MINUTES, selected)
    }

    @Test
    fun timeoutOptions_disabledWhileLockIsOff() {
        var invoked = false
        setContent(availableState().copy(lockEnabled = false), onTimeoutSelected = { invoked = true })

        composeRule.onNodeWithTag(TestTags.TIMEOUT_TEN_MINUTES).performClick()
        assertFalse(invoked)
    }
}
