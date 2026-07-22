package com.kratt.finanzas.presentation.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.presentation.common.IconCatalog
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.components.DropdownField

@Composable
fun CategoryFormRoute(categoryId: Long?, initialType: TransactionType, onDone: () -> Unit) {
    val viewModel = containerViewModel(key = "category_form_$categoryId") {
        CategoryFormViewModel(it.categoryRepository, it.transactionRepository, categoryId, initialType)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onDone() }
    CategoryFormScreen(
        state = state,
        onBack = onDone,
        onNameChange = viewModel::onNameChange,
        onTypeChange = viewModel::onTypeChange,
        onIconChange = viewModel::onIconChange,
        onSave = viewModel::onSave,
        onToggleActive = viewModel::onToggleActive,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormScreen(
    state: CategoryFormUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onIconChange: (String) -> Unit,
    onSave: () -> Unit,
    onToggleActive: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.CATEGORY_FORM_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(if (state.isEdit) R.string.edit_category_title else R.string.new_category_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.name_label)) },
                singleLine = true,
                isError = state.nameErrorRes != null,
                supportingText = state.nameErrorRes?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.CATEGORY_NAME_FIELD),
            )
            DropdownField(
                label = stringResource(R.string.type_label),
                options = listOf(TransactionType.EXPENSE, TransactionType.INCOME),
                selected = state.type,
                optionLabel = { stringResource(if (it == TransactionType.EXPENSE) R.string.filter_expenses else R.string.filter_incomes) },
                onSelected = onTypeChange,
                tag = "category_type_field",
            )
            if (state.typeNoticeRes != null) {
                Text(
                    text = stringResource(state.typeNoticeRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            DropdownField(
                label = stringResource(R.string.icon_label),
                options = IconCatalog.options,
                selected = IconCatalog.options.firstOrNull { it.key == state.iconKey },
                optionLabel = { stringResource(it.label) },
                onSelected = { onIconChange(it.key) },
                tag = "category_icon_field",
            )
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.SAVE_CATEGORY_BUTTON),
            ) {
                Text(stringResource(R.string.save_category))
            }
            if (state.isEdit) {
                OutlinedButton(
                    onClick = onToggleActive,
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.DEACTIVATE_CATEGORY_BUTTON),
                ) {
                    Text(stringResource(if (state.isActive) R.string.deactivate_category else R.string.reactivate_category))
                }
            }
        }
    }
}
