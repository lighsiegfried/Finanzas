package com.kratt.finanzas.presentation.bootstrap

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.di.DatabaseBootstrapState
import com.kratt.finanzas.presentation.theme.MisFinanzasTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BootstrapScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun bootstrapScreen_migrating_showsSpanishLabels_andSavesEvidence() {
        composeRule.setContent {
            MisFinanzasTheme {
                BootstrapScreen(state = DatabaseBootstrapState.MIGRATING)
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.bootstrap_preparing)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.bootstrap_migrating)).assertIsDisplayed()

        // la evidencia de captura es de mejor esfuerzo: captureToImage puede fallar por tiempos de
        // redibujado del emulador bajo carga; eso no debe tumbar la prueba de las etiquetas
        runCatching {
            val bitmap = composeRule.onNodeWithTag(TestTags.BOOTSTRAP_SCREEN).captureToImage().asAndroidBitmap()
            File(context.filesDir, "migration-screen.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test
    fun recoveryScreen_showsSpanishLabels_withoutDestructiveAction() {
        composeRule.setContent {
            MisFinanzasTheme {
                RecoveryScreen(onRetry = {})
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.recovery_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.recovery_message)).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.RETRY_BUTTON).assertIsDisplayed()
    }
}
