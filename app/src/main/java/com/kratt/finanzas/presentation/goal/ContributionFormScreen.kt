package com.kratt.finanzas.presentation.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.kratt.finanzas.domain.model.ContributionType
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.components.DateField
import com.kratt.finanzas.presentation.components.DropdownField
import java.time.LocalDate

@Composable
fun ContributionFormRoute(
    goalId: Long,
    onDone: () -> Unit,
) {
    val viewModel = containerViewModel(key = "contribution_form_$goalId") {
        ContributionFormViewModel(
            it.savingsGoalRepository,
            it.savingsContributionRepository,
            it.accountRepository,
            goalId,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    // cierra el formulario cuando el aporte ya quedo guardado
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    ContributionFormScreen(
        state = state,
        accounts = accounts,
        onBack = onDone,
        onTypeChange = viewModel::onTypeChange,
        onAmountChange = viewModel::onAmountChange,
        onDateChange = viewModel::onDateChange,
        onSourceAccountChange = viewModel::onSourceAccountChange,
        onNoteChange = viewModel::onNoteChange,
        onSave = viewModel::onSave,
    )
}

// formulario para registrar un aporte manual o por transferencia
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionFormScreen(
    state: ContributionFormUiState,
    accounts: List<Account>,
    onBack: () -> Unit,
    onTypeChange: (ContributionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSourceAccountChange: (Long) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val isTransfer = state.type == ContributionType.ACCOUNT_TRANSFER

    Scaffold(
        modifier = Modifier.testTag(TestTags.CONTRIBUTION_FORM_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.contribution_add)) },
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
            // eleccion del tipo de aporte; la transferencia solo si hay cuenta asociada
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.type == ContributionType.MANUAL_TRACKING,
                    onClick = { onTypeChange(ContributionType.MANUAL_TRACKING) },
                    label = { Text(stringResource(R.string.contribution_manual)) },
                )
                FilterChip(
                    selected = state.type == ContributionType.ACCOUNT_TRANSFER,
                    onClick = { onTypeChange(ContributionType.ACCOUNT_TRANSFER) },
                    enabled = state.transferEnabled,
                    label = { Text(stringResource(R.string.contribution_transfer)) },
                )
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.contribution_amount)) },
                prefix = { Text(stringResource(R.string.currency_symbol)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = ContributionFormError.INVALID_AMOUNT in state.errors,
                supportingText = if (ContributionFormError.INVALID_AMOUNT in state.errors) {
                    { Text(stringResource(R.string.error_contribution_amount)) }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.CONTRIBUTION_AMOUNT_FIELD),
            )

            DateField(
                date = state.date,
                onDateSelected = onDateChange,
                label = stringResource(R.string.contribution_date),
                tag = TestTags.CONTRIBUTION_FORM_SCREEN + "_date",
            )

            if (isTransfer) {
                DropdownField(
                    label = stringResource(R.string.contribution_source_account),
                    options = accounts,
                    selected = accounts.firstOrNull { it.id == state.sourceAccountId },
                    optionLabel = { it.name },
                    onSelected = { onSourceAccountChange(it.id) },
                    tag = TestTags.CONTRIBUTION_FORM_SCREEN + "_source",
                )
                if (ContributionFormError.ACCOUNT_REQUIRED in state.errors) {
                    Text(
                        text = stringResource(R.string.error_contribution_account),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (ContributionFormError.NO_LINKED_ACCOUNT in state.errors) {
                Text(
                    text = stringResource(R.string.error_contribution_no_linked_account),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.contribution_note)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.SAVE_CONTRIBUTION_BUTTON),
            ) {
                Text(stringResource(R.string.contribution_save))
            }
        }
    }
}
