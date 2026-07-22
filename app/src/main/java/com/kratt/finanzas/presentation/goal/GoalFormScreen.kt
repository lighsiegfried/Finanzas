package com.kratt.finanzas.presentation.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.usecase.SavingsGoalValidationError
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.components.DateField
import com.kratt.finanzas.presentation.components.DropdownField
import java.time.LocalDate

@Composable
fun GoalFormRoute(
    goalId: Long?,
    onDone: () -> Unit,
) {
    val viewModel = containerViewModel(key = "goal_form_$goalId") {
        GoalFormViewModel(it.savingsGoalRepository, it.accountRepository, goalId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    // cierra el formulario cuando la meta ya quedo guardada
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    GoalFormScreen(
        state = state,
        accounts = accounts,
        isEditing = viewModel.isEditing,
        onBack = onDone,
        onNameChange = viewModel::onNameChange,
        onTargetChange = viewModel::onTargetChange,
        onTargetDateChange = viewModel::onTargetDateChange,
        onLinkedAccountChange = viewModel::onLinkedAccountChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSave = viewModel::onSave,
    )
}

// formulario para crear o editar una meta de ahorro
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormScreen(
    state: GoalFormUiState,
    accounts: List<Account>,
    isEditing: Boolean,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onTargetDateChange: (LocalDate?) -> Unit,
    onLinkedAccountChange: (Long?) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.GOAL_FORM_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(if (isEditing) R.string.goal_edit else R.string.goal_new))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.goal_name_label)) },
                singleLine = true,
                isError = SavingsGoalValidationError.NAME_REQUIRED in state.errors,
                supportingText = if (SavingsGoalValidationError.NAME_REQUIRED in state.errors) {
                    { Text(stringResource(R.string.error_goal_name)) }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.GOAL_NAME_FIELD),
            )

            OutlinedTextField(
                value = state.targetText,
                onValueChange = onTargetChange,
                label = { Text(stringResource(R.string.goal_target_label)) },
                prefix = { Text(stringResource(R.string.currency_symbol)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = SavingsGoalValidationError.INVALID_TARGET in state.errors ||
                    SavingsGoalValidationError.AMOUNT_TOO_LARGE in state.errors,
                supportingText = targetErrorRes(state.errors)?.let { res -> { Text(stringResource(res)) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.GOAL_TARGET_FIELD),
            )

            // la fecha objetivo es opcional; se puede limpiar para dejarla sin fecha
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.targetDate != null) {
                    DateField(
                        date = state.targetDate,
                        onDateSelected = { onTargetDateChange(it) },
                        label = stringResource(R.string.goal_target_date),
                        tag = TestTags.GOAL_TARGET_FIELD + "_date",
                    )
                    Row {
                        TextButton(onClick = { onTargetDateChange(null) }) {
                            Text(stringResource(R.string.goal_no_target_date))
                        }
                    }
                } else {
                    // sin fecha usamos hoy como base al abrir el selector
                    DateField(
                        date = LocalDate.now(),
                        onDateSelected = { onTargetDateChange(it) },
                        label = stringResource(R.string.goal_no_target_date),
                        tag = TestTags.GOAL_TARGET_FIELD + "_date",
                    )
                }
                if (SavingsGoalValidationError.TARGET_DATE_BEFORE_START in state.errors) {
                    Text(
                        text = stringResource(R.string.error_goal_target_date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // opcion de cuenta asociada; null significa sin cuenta y solo registra el avance
            val accountOptions: List<Account?> = listOf(null) + accounts
            DropdownField(
                label = stringResource(R.string.goal_linked_account),
                options = accountOptions,
                selected = accounts.firstOrNull { it.id == state.linkedAccountId },
                optionLabel = { it?.name ?: stringResource(R.string.goal_no_linked_account_option) },
                onSelected = { onLinkedAccountChange(it?.id) },
                tag = TestTags.GOAL_FORM_SCREEN + "_account",
            )
            if (state.linkedAccountId == null) {
                Text(
                    text = stringResource(R.string.goal_no_account_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.goal_description)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.SAVE_GOAL_BUTTON),
            ) {
                Text(stringResource(R.string.goal_save))
            }
        }
    }
}

// el objetivo puede fallar por invalido o por ser demasiado grande
private fun targetErrorRes(errors: Set<SavingsGoalValidationError>): Int? = when {
    SavingsGoalValidationError.AMOUNT_TOO_LARGE in errors -> R.string.error_amount_too_large
    SavingsGoalValidationError.INVALID_TARGET in errors -> R.string.error_goal_target
    else -> null
}
