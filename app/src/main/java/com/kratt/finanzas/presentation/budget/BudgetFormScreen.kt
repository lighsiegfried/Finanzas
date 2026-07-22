package com.kratt.finanzas.presentation.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.components.DropdownField

@Composable
fun BudgetFormRoute(year: Int, month: Int, budgetId: Long?, onDone: () -> Unit) {
    val viewModel = containerViewModel(key = "budget_form_${year}_${month}_$budgetId") {
        BudgetFormViewModel(it.budgetRepository, it.categoryRepository, year, month, budgetId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onDone() }
    BudgetFormScreen(state, onDone, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormScreen(state: BudgetFormUiState, onBack: () -> Unit, viewModel: BudgetFormViewModel) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.BUDGET_FORM_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(if (state.isEdit) R.string.edit_budget else R.string.new_budget)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val categoryLabel: @Composable (Category) -> String = { it.name }
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.isEdit) {
                ScopeOption(stringResource(R.string.budget_overall), state.isOverall) { viewModel.onScopeChange(true) }
                ScopeOption(stringResource(R.string.budget_category), !state.isOverall) { viewModel.onScopeChange(false) }
            }
            if (!state.isOverall) {
                DropdownField(
                    label = stringResource(R.string.category_label),
                    options = state.categories, selected = state.categories.firstOrNull { it.id == state.categoryId },
                    optionLabel = categoryLabel, onSelected = { viewModel.onCategorySelected(it.id) },
                    tag = TestTags.BUDGET_CATEGORY_FIELD,
                )
                state.categoryErrorRes?.let { Text(stringResource(it), color = androidx.compose.material3.MaterialTheme.colorScheme.error, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
            }
            OutlinedTextField(
                value = state.limitText, onValueChange = viewModel::onLimitChange, singleLine = true,
                label = { Text(stringResource(R.string.limit_amount_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.amountErrorRes != null,
                supportingText = state.amountErrorRes?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.BUDGET_LIMIT_FIELD),
            )
            OutlinedTextField(
                value = state.warningText, onValueChange = viewModel::onWarningChange, singleLine = true,
                label = { Text(stringResource(R.string.warning_at_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag(TestTags.BUDGET_WARNING_FIELD),
            )
            Button(onClick = viewModel::onSave, modifier = Modifier.fillMaxWidth().testTag(TestTags.SAVE_BUDGET_BUTTON)) {
                Text(stringResource(R.string.save_budget))
            }
        }
    }
}

@Composable
private fun ScopeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 12.dp))
    }
}
