package com.kratt.finanzas.e2e

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.FinanzasApplication
import com.kratt.finanzas.MainActivity
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.data.local.DefaultDataSeeder
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// prueba de extremo a extremo sobre la app real con su base local
@RunWith(AndroidJUnit4::class)
class AddTransactionFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetDatabase() {
        // deja la base solo con los datos iniciales usando los daos
        // para que room avise a los flows y la ui se refresque
        val app = context.applicationContext as FinanzasApplication
        // espera a que la base cifrada termine de arrancar antes de tocarla
        val start = System.currentTimeMillis()
        while (app.container.databaseState.value != com.kratt.finanzas.di.DatabaseBootstrapState.READY) {
            if (System.currentTimeMillis() - start > 15_000) error("database bootstrap timeout")
            Thread.sleep(50)
        }
        val db = app.container.database
        runBlocking {
            db.clearAllTables()
            db.accountDao().insert(DefaultDataSeeder.defaultAccount(1L))
            db.categoryDao().insertAll(DefaultDataSeeder.defaultCategories(1L))
            // estas pruebas corren con el bloqueo apagado
            app.container.securityPreferencesRepository.setAppLockEnabled(false)
            // marca la configuracion inicial como completa para no ver la bienvenida
            app.container.onboardingPreferences.setCompleted()
        }
        app.container.appLockManager.unlock()
        // vuelve a crear la activity para que la navegacion arranque en el resumen
        composeRule.activityRule.scenario.recreate()
    }

    private fun getString(res: Int): String = context.getString(res)

    private fun openAddTransactionScreen() {
        // espera a que pase la puerta de bloqueo del arranque
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(TestTags.ADD_TRANSACTION_BUTTON)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.ADD_TRANSACTION_BUTTON).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(TestTags.ADD_TRANSACTION_SCREEN)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun addScreen_showsRequiredFormLabelsInSpanish() {
        openAddTransactionScreen()

        listOf(
            R.string.transaction_type_expense,
            R.string.transaction_type_income,
            R.string.amount_label,
            R.string.account_label,
            R.string.category_label,
            R.string.description_label,
            R.string.date_label,
            R.string.save_transaction,
            R.string.cancel_action,
        ).forEach { res ->
            composeRule.onNodeWithText(getString(res)).assertExists()
        }
    }

    @Test
    fun saveWithEmptyAmount_showsValidationMessages() {
        openAddTransactionScreen()

        composeRule.onNodeWithTag(TestTags.SAVE_BUTTON).performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(getString(R.string.error_invalid_amount))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(getString(R.string.error_invalid_amount)).assertExists()
        // la cuenta viene preseleccionada, la categoria no
        composeRule.onNodeWithText(getString(R.string.error_missing_category)).assertExists()
    }

    @Test
    fun saveValidExpense_updatesSummaryAndAppearsInHistory() {
        val description = "Compra de prueba"
        openAddTransactionScreen()

        // espera a que la cuenta efectivo este cargada y elegida por defecto
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Efectivo").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(TestTags.AMOUNT_FIELD).performTextInput("125.75")

        composeRule.onNodeWithTag(TestTags.CATEGORY_FIELD).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Alimentación").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Alimentación").performClick()

        composeRule.onNodeWithTag(TestTags.DESCRIPTION_FIELD).performScrollTo()
            .performTextInput(description)

        composeRule.onNodeWithTag(TestTags.SAVE_BUTTON).performScrollTo().performClick()

        // debe volver al resumen con los totales ya actualizados
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(TestTags.SUMMARY_SCREEN)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Q125.75").fetchSemanticsNodes().isNotEmpty()
        }
        // el balance del mes y la fila reciente muestran el monto con signo
        composeRule.onAllNodesWithText("-Q125.75").onFirst().assertExists()
        composeRule.onNodeWithText(description).assertExists()

        // abre el historial y confirma que el movimiento tambien aparece ahi
        composeRule.onNodeWithTag(TestTags.NAV_TRANSACTIONS).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(TestTags.TRANSACTIONS_SCREEN)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(description).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(description).assertIsDisplayed()
    }
}
