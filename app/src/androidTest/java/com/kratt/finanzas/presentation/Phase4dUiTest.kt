package com.kratt.finanzas.presentation

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.di.DatabaseBootstrapState
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.csv.ImportFileError
import com.kratt.finanzas.domain.usecase.csv.ImportPreview
import com.kratt.finanzas.domain.usecase.csv.ImportRowError
import com.kratt.finanzas.domain.usecase.csv.ImportSummary
import com.kratt.finanzas.domain.usecase.csv.ImportedMovement
import com.kratt.finanzas.domain.usecase.csv.RowResult
import com.kratt.finanzas.presentation.about.AboutScreen
import com.kratt.finanzas.presentation.bootstrap.BootstrapScreen
import com.kratt.finanzas.presentation.importer.ImportScreen
import com.kratt.finanzas.presentation.importer.ImportUiState
import com.kratt.finanzas.presentation.theme.MisFinanzasTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase4dUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun bootstrapScreen_showsBrandingAndPoweredBy() {
        composeRule.setContent { MisFinanzasTheme { BootstrapScreen(state = DatabaseBootstrapState.PREPARING) } }
        composeRule.onNodeWithText(context.getString(R.string.app_name)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.splash_subtitle)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.powered_by)).assertIsDisplayed()
    }

    @Test
    fun aboutScreen_showsPoweredBy() {
        composeRule.setContent { MisFinanzasTheme { AboutScreen(onBack = {}, onOpenPrivacy = {}, onOpenLicenses = {}) } }
        composeRule.onNodeWithText(context.getString(R.string.powered_by)).assertIsDisplayed()
    }

    private fun movement(row: Int) = ImportedMovement(
        rowNumber = row, type = TransactionType.EXPENSE, date = LocalDate.of(2026, 7, 5),
        description = "Almuerzo", amountCents = 8_500L, accountId = 1L, categoryId = 10L, destinationAccountId = null,
    )

    @Test
    fun importScreen_showsPreviewCounts() {
        val preview = ImportPreview(
            valid = listOf(movement(1), movement(2)),
            duplicates = listOf(movement(2)),
            errors = listOf(RowResult.Invalid(3, ImportRowError.INVALID_TYPE)),
        )
        renderImport(ImportUiState(preview = preview))
        composeRule.onNodeWithTag(TestTags.IMPORT_PREVIEW).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.valid_rows)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.possible_duplicates)).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.IMPORT_BUTTON).assertIsDisplayed()
    }

    @Test
    fun importScreen_invalidFile_showsErrorAndNoImportButton() {
        renderImport(ImportUiState(preview = ImportPreview(fileError = ImportFileError.MISSING_REQUIRED_COLUMN)))
        composeRule.onNodeWithText(context.getString(R.string.error_missing_column)).assertIsDisplayed()
    }

    @Test
    fun importScreen_summary_showsCompleted() {
        renderImport(ImportUiState(summary = ImportSummary(imported = 3, skippedDuplicates = 1, errors = 0, duplicatesDetected = 1)))
        composeRule.onNodeWithTag(TestTags.IMPORT_SUMMARY).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.import_completed)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.imported_movements)).assertIsDisplayed()
    }

    private fun renderImport(state: ImportUiState) {
        composeRule.setContent {
            MisFinanzasTheme {
                ImportScreen(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = {}, onSelectFile = {}, onDownloadTemplate = {}, onConfirmImport = {},
                    onOpenBackup = {}, onCancel = {}, onFinish = {},
                )
            }
        }
    }
}
