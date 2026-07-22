package com.kratt.finanzas.presentation.movement

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.components.DateField
import com.kratt.finanzas.presentation.components.DropdownField

@Composable
fun EditMovementRoute(transactionId: Long, onDone: () -> Unit) {
    val viewModel = containerViewModel(key = "edit_movement_$transactionId") {
        EditMovementViewModel(it.accountRepository, it.categoryRepository, it.transactionRepository, it.editTransaction, transactionId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onDone() }
    EditMovementScreen(
        state = state,
        onBack = onDone,
        onTypeChange = viewModel::onTypeChange,
        onAmountChange = viewModel::onAmountChange,
        onAccountSelected = viewModel::onAccountSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onDescriptionChange = viewModel::onDescriptionChange,
        onDateSelected = viewModel::onDateSelected,
        onSave = viewModel::onSave,
        onSaveErrorShown = viewModel::onSaveErrorShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMovementScreen(
    state: EditMovementUiState,
    onBack: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onAccountSelected: (Long) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onSave: () -> Unit,
    onSaveErrorShown: () -> Unit,
) {
    var showDiscard by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.saveErrorRes) {
        state.saveErrorRes?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            onSaveErrorShown()
        }
    }

    // intercepta el boton atras para avisar de los cambios sin guardar
    fun attemptBack() {
        if (state.dirty && !state.isSaved) showDiscard = true else onBack()
    }
    BackHandler(enabled = state.dirty && !state.isSaved) { showDiscard = true }

    Scaffold(
        modifier = Modifier.testTag(TestTags.EDIT_MOVEMENT_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.edit_movement_title)) },
                navigationIcon = {
                    IconButton(onClick = { attemptBack() }) {
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
            DropdownField(
                label = stringResource(R.string.type_label),
                options = listOf(TransactionType.EXPENSE, TransactionType.INCOME),
                selected = state.type,
                optionLabel = { stringResource(if (it == TransactionType.EXPENSE) R.string.filter_expenses else R.string.filter_incomes) },
                onSelected = onTypeChange,
                tag = "edit_type_field",
            )
            OutlinedTextField(
                value = state.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.amount_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.amountErrorRes != null,
                supportingText = state.amountErrorRes?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.AMOUNT_FIELD),
            )
            DropdownField(
                label = stringResource(R.string.account_label),
                options = state.accounts,
                selected = state.accounts.firstOrNull { it.id == state.selectedAccountId },
                optionLabel = accountLabel,
                onSelected = { onAccountSelected(it.id) },
                tag = TestTags.ACCOUNT_FIELD,
            )
            DropdownField(
                label = stringResource(R.string.category_label),
                options = state.categories,
                selected = state.categories.firstOrNull { it.id == state.selectedCategoryId },
                optionLabel = categoryLabel,
                onSelected = { onCategorySelected(it.id) },
                tag = TestTags.CATEGORY_FIELD,
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.description_label)) },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.DESCRIPTION_FIELD),
            )
            DateField(
                date = state.date,
                onDateSelected = onDateSelected,
                label = stringResource(R.string.date_label),
                tag = TestTags.DATE_FIELD,
            )
            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.SAVE_CHANGES_BUTTON),
            ) {
                Text(stringResource(R.string.save_changes))
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.discard_changes)) },
            text = { Text(stringResource(R.string.discard_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    onBack()
                }) { Text(stringResource(R.string.discard_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) { Text(stringResource(R.string.keep_editing)) }
            },
        )
    }
}
