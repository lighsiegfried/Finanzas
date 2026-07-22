package com.kratt.finanzas.presentation.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.presentation.common.containerViewModel

// un paso de la configuracion inicial; la accion abre la pantalla real ya existente
private data class OnboardingStep(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val actionLabel: Int? = null,
    val onAction: (() -> Unit)? = null,
    val extraBody: Int? = null,
)

@Composable
fun OnboardingRoute(
    onFinish: () -> Unit,
    onAddAccount: () -> Unit,
    onOpenCategories: () -> Unit,
    onAddRecurring: () -> Unit,
    onAddBudget: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenImport: () -> Unit,
) {
    val viewModel = containerViewModel { OnboardingViewModel(it.onboardingPreferences) }
    val finish = {
        viewModel.complete()
        onFinish()
    }
    val steps = listOf(
        OnboardingStep(R.string.onboarding_welcome_title, R.string.onboarding_welcome_body, extraBody = R.string.onboarding_local_data),
        OnboardingStep(R.string.onboarding_accounts_title, R.string.onboarding_accounts_body, R.string.add_account, onAddAccount),
        OnboardingStep(R.string.onboarding_categories_title, R.string.onboarding_categories_body, R.string.categories_title, onOpenCategories),
        OnboardingStep(R.string.onboarding_recurring_title, R.string.onboarding_recurring_body, R.string.new_recurring_title, onAddRecurring),
        OnboardingStep(R.string.onboarding_budget_title, R.string.onboarding_budget_body, R.string.add_budget, onAddBudget),
        OnboardingStep(R.string.onboarding_security_title, R.string.onboarding_security_body, R.string.onboarding_configure_security, onOpenSecurity),
        OnboardingStep(R.string.onboarding_backup_title, R.string.onboarding_backup_body, R.string.backup_create, onOpenBackup),
        OnboardingStep(R.string.onboarding_import_title, R.string.onboarding_import_body, R.string.import_title, onOpenImport),
    )
    OnboardingScreen(steps = steps, onFinish = finish)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingScreen(steps: List<OnboardingStep>, onFinish: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index]
    val isFirst = index == 0
    val isLast = index == steps.lastIndex

    Scaffold(
        modifier = Modifier.testTag(TestTags.ONBOARDING_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.onboarding_configure_subtitle)) },
                actions = {
                    // permite salir de la configuracion en cualquier momento
                    if (!isFirst) {
                        TextButton(onClick = onFinish, modifier = Modifier.testTag(TestTags.ONBOARDING_SKIP_BUTTON)) {
                            Text(stringResource(R.string.onboarding_skip))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(step.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(stringResource(step.body), style = MaterialTheme.typography.bodyLarge)
            step.extraBody?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            // boton que abre la pantalla real reutilizando el modulo existente
            if (step.actionLabel != null && step.onAction != null) {
                OutlinedButton(onClick = step.onAction, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(step.actionLabel))
                }
            }

            when {
                isFirst -> {
                    Button(
                        onClick = { index++ },
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.ONBOARDING_START_BUTTON),
                    ) { Text(stringResource(R.string.onboarding_start)) }
                    TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth().testTag(TestTags.ONBOARDING_SKIP_BUTTON)) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }
                isLast -> {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.ONBOARDING_FINISH_BUTTON),
                    ) { Text(stringResource(R.string.onboarding_finish_setup)) }
                }
                else -> {
                    Button(
                        onClick = { index++ },
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.ONBOARDING_CONTINUE_BUTTON),
                    ) { Text(stringResource(R.string.onboarding_continue)) }
                    TextButton(onClick = { index++ }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }
            }
        }
    }
}
