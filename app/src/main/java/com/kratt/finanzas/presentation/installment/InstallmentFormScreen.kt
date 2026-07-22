package com.kratt.finanzas.presentation.installment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.kratt.finanzas.common.CurrencyFormatter
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.components.DateField
import com.kratt.finanzas.presentation.components.DropdownField

@Composable
fun InstallmentFormRoute(onDone: () -> Unit) {
    val viewModel = containerViewModel {
        InstallmentFormViewModel(it.accountRepository, it.categoryRepository, it.installmentRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onDone() }
    InstallmentFormScreen(
        state = state,
        onBack = onDone,
        onNameChange = viewModel::onNameChange,
        onAccountSelected = viewModel::onAccountSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onTotalChange = viewModel::onTotalChange,
        onCountChange = viewModel::onCountChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onDateSelected = viewModel::onDateSelected,
        onSave = viewModel::onSave,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentFormScreen(
    state: InstallmentFormUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onAccountSelected: (Long) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onTotalChange: (String) -> Unit,
    onCountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.INSTALLMENT_FORM_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.new_installment_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val accountLabel: @Composable (Account) -> String = { it.name }
        val categoryLabel: @Composable (Category) -> String = { it.name }
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name, onValueChange = onNameChange, singleLine = true,
                label = { Text(stringResource(R.string.installment_name_label)) },
                isError = state.nameErrorRes != null,
                supportingText = state.nameErrorRes?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.INSTALLMENT_NAME_FIELD),
            )
            DropdownField(
                label = stringResource(R.string.installment_account_label),
                options = state.accounts, selected = state.accounts.firstOrNull { it.id == state.accountId },
                optionLabel = accountLabel, onSelected = { onAccountSelected(it.id) }, tag = TestTags.INSTALLMENT_ACCOUNT_FIELD,
            )
            DropdownField(
                label = stringResource(R.string.category_label),
                options = state.categories, selected = state.categories.firstOrNull { it.id == state.categoryId },
                optionLabel = categoryLabel, onSelected = { onCategorySelected(it.id) }, tag = TestTags.INSTALLMENT_CATEGORY_FIELD,
            )
            OutlinedTextField(
                value = state.totalText, onValueChange = onTotalChange, singleLine = true,
                label = { Text(stringResource(R.string.total_amount_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.totalErrorRes != null,
                supportingText = state.totalErrorRes?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.INSTALLMENT_TOTAL_FIELD),
            )
            OutlinedTextField(
                value = state.countText, onValueChange = onCountChange, singleLine = true,
                label = { Text(stringResource(R.string.installment_count_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.countErrorRes != null,
                supportingText = state.countErrorRes?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.INSTALLMENT_COUNT_FIELD),
            )
            state.installmentAmountCents?.let { perInstallment ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.installment_amount_label), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.format(perInstallment), style = MaterialTheme.typography.titleMedium)
                }
            }
            DateField(
                date = state.firstDueDate, onDateSelected = onDateSelected,
                label = stringResource(R.string.first_due_date_label), tag = "installment_date_field",
            )
            OutlinedTextField(
                value = state.description, onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.description_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().testTag(TestTags.SAVE_INSTALLMENT_BUTTON)) {
                Text(stringResource(R.string.save_installment))
            }
        }
    }
}
