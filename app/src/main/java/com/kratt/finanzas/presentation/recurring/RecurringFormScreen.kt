package com.kratt.finanzas.presentation.recurring

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.PostingMode
import com.kratt.finanzas.domain.model.RecurrenceType
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.components.DateField
import com.kratt.finanzas.presentation.components.DropdownField

@Composable
fun RecurringFormRoute(onDone: () -> Unit) {
    val viewModel = containerViewModel {
        RecurringFormViewModel(it.accountRepository, it.categoryRepository, it.recurringRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onDone() }
    RecurringFormScreen(state, onDone, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormScreen(
    state: RecurringFormUiState,
    onBack: () -> Unit,
    viewModel: RecurringFormViewModel,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.RECURRING_FORM_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.new_recurring_title)) },
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
                value = state.name, onValueChange = viewModel::onNameChange, singleLine = true,
                label = { Text(stringResource(R.string.name_label)) },
                isError = state.nameErrorRes != null,
                supportingText = state.nameErrorRes?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.RECURRING_NAME_FIELD),
            )
            DropdownField(
                label = stringResource(R.string.type_label),
                options = listOf(TransactionType.EXPENSE, TransactionType.INCOME), selected = state.type,
                optionLabel = { stringResource(if (it == TransactionType.EXPENSE) R.string.recurring_expense else R.string.recurring_income) },
                onSelected = viewModel::onTypeChange, tag = "recurring_type_field",
            )
            DropdownField(
                label = stringResource(R.string.account_label),
                options = state.accounts, selected = state.accounts.firstOrNull { it.id == state.accountId },
                optionLabel = accountLabel, onSelected = { viewModel.onAccountSelected(it.id) }, tag = "recurring_account_field",
            )
            DropdownField(
                label = stringResource(R.string.category_label),
                options = state.categories, selected = state.categories.firstOrNull { it.id == state.categoryId },
                optionLabel = categoryLabel, onSelected = { viewModel.onCategorySelected(it.id) }, tag = "recurring_category_field",
            )
            OutlinedTextField(
                value = state.amountText, onValueChange = viewModel::onAmountChange, singleLine = true,
                label = { Text(stringResource(R.string.amount_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.amountErrorRes != null,
                supportingText = state.amountErrorRes?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.RECURRING_AMOUNT_FIELD),
            )
            DropdownField(
                label = stringResource(R.string.frequency_label),
                options = listOf(RecurrenceType.WEEKLY, RecurrenceType.MONTHLY, RecurrenceType.YEARLY), selected = state.recurrenceType,
                optionLabel = {
                    stringResource(
                        when (it) {
                            RecurrenceType.WEEKLY -> R.string.freq_weekly
                            RecurrenceType.MONTHLY -> R.string.freq_monthly
                            RecurrenceType.YEARLY -> R.string.freq_yearly
                        },
                    )
                },
                onSelected = viewModel::onRecurrenceChange, tag = "recurring_frequency_field",
            )
            OutlinedTextField(
                value = state.intervalText, onValueChange = viewModel::onIntervalChange, singleLine = true,
                label = { Text(stringResource(R.string.every_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            DateField(
                date = state.startDate, onDateSelected = viewModel::onStartDateSelected,
                label = stringResource(R.string.start_date_label), tag = "recurring_start_date_field",
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.no_end_date), modifier = Modifier.weight(1f))
                Switch(checked = !state.hasEndDate, onCheckedChange = { viewModel.onToggleEndDate(!it) })
            }
            if (state.hasEndDate) {
                DateField(
                    date = state.endDate, onDateSelected = viewModel::onEndDateSelected,
                    label = stringResource(R.string.end_date_label), tag = "recurring_end_date_field",
                )
                state.endDateErrorRes?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            DropdownField(
                label = stringResource(R.string.posting_mode_label),
                options = listOf(PostingMode.REQUIRE_CONFIRMATION, PostingMode.AUTO_POST), selected = state.postingMode,
                optionLabel = { stringResource(if (it == PostingMode.REQUIRE_CONFIRMATION) R.string.posting_confirm else R.string.posting_auto) },
                onSelected = viewModel::onPostingModeChange, tag = "recurring_posting_field",
            )
            Button(onClick = viewModel::onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().testTag(TestTags.SAVE_RECURRING_BUTTON)) {
                Text(stringResource(R.string.save_recurring))
            }
        }
    }
}
