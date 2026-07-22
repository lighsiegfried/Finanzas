package com.kratt.finanzas.e2e

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.FinanzasApplication
import com.kratt.finanzas.MainActivity
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.LockTimeout
import com.kratt.finanzas.security.LockSessionState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// valida que la puerta de bloqueo tape las pantallas financieras
@RunWith(AndroidJUnit4::class)
class LockGateTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val app: FinanzasApplication
        get() = context.applicationContext as FinanzasApplication

    @Before
    fun startUnlocked() {
        runBlocking {
            app.container.securityPreferencesRepository.setAppLockEnabled(false)
            app.container.securityPreferencesRepository.setLockTimeout(LockTimeout.SESSION)
            // evita la pantalla de bienvenida para llegar directo al resumen
            app.container.onboardingPreferences.setCompleted()
        }
        app.container.appLockManager.unlock()
        // vuelve a crear la activity para que la navegacion lea la configuracion completada
        composeRule.activityRule.scenario.recreate()
    }

    @After
    fun cleanup() {
        runBlocking {
            app.container.securityPreferencesRepository.setAppLockEnabled(false)
        }
        app.container.appLockManager.unlock()
    }

    // habilita el bloqueo y espera a que el manager realmente bloquee
    private fun enableLockAndLock() {
        runBlocking {
            app.container.securityPreferencesRepository.setAppLockEnabled(true)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            app.container.appLockManager.lockNow()
            app.container.appLockManager.sessionState.value == LockSessionState.LOCKED
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(TestTags.LOCK_SCREEN)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun lockedApp_blocksFinancialNavigation_andShowsNoAmounts() {
        enableLockAndLock()

        composeRule.onNodeWithTag(TestTags.LOCK_SCREEN).assertExists()
        // ninguna pantalla financiera ni barra de navegacion accesible
        composeRule.onAllNodesWithTag(TestTags.SUMMARY_SCREEN).assertCountEquals(0)
        composeRule.onAllNodesWithTag(TestTags.NAV_TRANSACTIONS).assertCountEquals(0)
        composeRule.onAllNodesWithTag(TestTags.ADD_TRANSACTION_BUTTON).assertCountEquals(0)
        // sin montos ni etiquetas financieras en la pantalla de bloqueo
        composeRule.onAllNodesWithText("Q0.00").assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.monthly_balance_label))
            .assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.recent_transactions_title))
            .assertCountEquals(0)
    }

    @Test
    fun successfulUnlock_revealsSummary() {
        enableLockAndLock()

        // simula el resultado exitoso del dialogo del sistema
        app.container.appLockManager.unlock()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(TestTags.SUMMARY_SCREEN)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.SUMMARY_SCREEN).assertExists()
    }

    @Test
    fun withoutSuccessfulAuth_appStaysLocked() {
        enableLockAndLock()

        // sin autenticacion exitosa el estado no cambia, cancelar deja todo igual
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.LOCK_SCREEN).assertExists()
        composeRule.onAllNodesWithTag(TestTags.SUMMARY_SCREEN).assertCountEquals(0)
    }

    @Test
    fun appWorksNormally_whenLockDisabled() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(TestTags.SUMMARY_SCREEN)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.SUMMARY_SCREEN).assertExists()
        composeRule.onNodeWithTag(TestTags.ADD_TRANSACTION_BUTTON).assertExists()
    }
}
