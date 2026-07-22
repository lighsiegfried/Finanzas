package com.kratt.finanzas.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.presentation.common.AccountTypeLabels
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.components.DropdownField

@Composable
fun AccountFormRoute(accountId: Long?, onDone: () -> Unit) {
    val viewModel = containerViewModel(key = "account_form_$accountId") {
        AccountFormViewModel(it.accountRepository, accountId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AccountFormScreen(
        state = state,
        onBack = onDone,
        onNameChange = viewModel::onNameChange,
        onTypeChange = viewModel::onTypeChange,
        onInitialBalanceChange = viewModel::onInitialBalanceChange,
        onCreditLimitChange = viewModel::onCreditLimitChange,
        onLastFourChange = viewModel::onLastFourChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSave = viewModel::onSave,
        onToggleActive = viewModel::onToggleActive,
        onSaved = onDone,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormScreen(
    state: AccountFormUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (AccountType) -> Unit,
    onInitialBalanceChange: (String) -> Unit,
    onCreditLimitChange: (String) -> Unit,
    onLastFourChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onToggleActive: () -> Unit,
    onSaved: () -> Unit,
) {
    if (state.isSaved) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onSaved() }
    }
    var showDeactivate by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.testTag(TestTags.ACCOUNT_FORM_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(if (state.isEdit) R.string.edit_account_title else R.string.new_account_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.name_label)) },
                singleLine = true,
                isError = state.nameErrorRes != null,
                supportingText = state.nameErrorRes?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.ACCOUNT_NAME_FIELD),
            )
            DropdownField(
                label = stringResource(R.string.account_type_label),
                options = AccountTypeLabels.ordered,
                selected = state.type,
                optionLabel = { stringResource(AccountTypeLabels.labelOf(it)) },
                onSelected = onTypeChange,
                tag = TestTags.ACCOUNT_TYPE_FIELD,
            )
            OutlinedTextField(
                value = state.initialBalanceText,
                onValueChange = onInitialBalanceChange,
                label = { Text(stringResource(R.string.initial_balance_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.amountErrorRes != null,
                supportingText = state.amountErrorRes?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.ACCOUNT_INITIAL_BALANCE_FIELD),
            )
            if (state.type == AccountType.CREDIT_CARD) {
                OutlinedTextField(
                    value = state.creditLimitText,
                    onValueChange = onCreditLimitChange,
                    label = { Text(stringResource(R.string.credit_limit_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.ACCOUNT_CREDIT_LIMIT_FIELD),
                )
                OutlinedTextField(
                    value = state.lastFour,
                    onValueChange = onLastFourChange,
                    label = { Text(stringResource(R.string.last_four_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = state.lastFourErrorRes != null,
                    supportingText = state.lastFourErrorRes?.let { { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.ACCOUNT_LAST_FOUR_FIELD),
                )
            }
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.description_label)) },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.ACCOUNT_DESCRIPTION_FIELD),
            )
            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.SAVE_ACCOUNT_BUTTON),
            ) {
                Text(stringResource(R.string.save_account))
            }
            if (state.isEdit) {
                OutlinedButton(
                    onClick = { if (state.isActive) showDeactivate = true else onToggleActive() },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.DEACTIVATE_ACCOUNT_BUTTON),
                ) {
                    Text(stringResource(if (state.isActive) R.string.deactivate_account else R.string.reactivate_account))
                }
            }
        }
    }

    if (showDeactivate) {
        AlertDialog(
            onDismissRequest = { showDeactivate = false },
            title = { Text(stringResource(R.string.deactivate_account)) },
            text = { Text(stringResource(R.string.deactivate_account_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeactivate = false
                    onToggleActive()
                }) { Text(stringResource(R.string.deactivate_action)) }
            },
            dismissButton = { TextButton(onClick = { showDeactivate = false }) { Text(stringResource(R.string.cancel_action)) } },
        )
    }
}
