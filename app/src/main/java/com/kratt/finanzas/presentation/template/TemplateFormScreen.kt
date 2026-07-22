package com.kratt.finanzas.presentation.template

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
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.TemplateValidationError
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.components.DropdownField

@Composable
fun TemplateFormRoute(
    templateId: Long?,
    onDone: () -> Unit,
) {
    val viewModel = containerViewModel(key = "template_form_${templateId}") {
        TemplateFormViewModel(
            it.transactionTemplateRepository,
            it.accountRepository,
            it.categoryRepository,
            templateId,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    // cierra el formulario cuando la plantilla ya quedo guardada
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    TemplateFormScreen(
        state = state,
        accounts = accounts,
        categories = categories,
        isEditing = viewModel.isEditing,
        onBack = onDone,
        onNameChange = viewModel::onNameChange,
        onTypeChange = viewModel::onTypeChange,
        onAccountSelected = viewModel::onAccountSelected,
        onDestinationSelected = viewModel::onDestinationSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onAmountChange = viewModel::onAmountChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSave = viewModel::onSave,
    )
}

// formulario para crear o editar una plantilla de movimiento
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateFormScreen(
    state: TemplateFormUiState,
    accounts: List<Account>,
    categories: List<Category>,
    isEditing: Boolean,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onAccountSelected: (Long) -> Unit,
    onDestinationSelected: (Long) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val isTransfer = state.type == TransactionType.TRANSFER

    Scaffold(
        modifier = Modifier.testTag(TestTags.TEMPLATE_FORM_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(if (isEditing) R.string.template_edit else R.string.template_new))
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
                label = { Text(stringResource(R.string.template_name_label)) },
                singleLine = true,
                isError = TemplateValidationError.NAME_REQUIRED in state.errors ||
                    TemplateValidationError.DUPLICATE_NAME in state.errors,
                supportingText = nameErrorRes(state.errors)?.let { res -> { Text(stringResource(res)) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.TEMPLATE_NAME_FIELD),
            )

            // selector de tipo con tres opciones
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.template_type_label))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.type == TransactionType.EXPENSE,
                        onClick = { onTypeChange(TransactionType.EXPENSE) },
                        label = { Text(stringResource(R.string.transaction_type_expense)) },
                    )
                    FilterChip(
                        selected = state.type == TransactionType.INCOME,
                        onClick = { onTypeChange(TransactionType.INCOME) },
                        label = { Text(stringResource(R.string.transaction_type_income)) },
                    )
                    FilterChip(
                        selected = state.type == TransactionType.TRANSFER,
                        onClick = { onTypeChange(TransactionType.TRANSFER) },
                        label = { Text(stringResource(R.string.filter_transfers)) },
                    )
                }
            }

            DropdownField(
                label = stringResource(if (isTransfer) R.string.template_source_account else R.string.template_account),
                options = accounts,
                selected = accounts.firstOrNull { it.id == state.accountId },
                optionLabel = { it.name },
                onSelected = { onAccountSelected(it.id) },
                tag = TestTags.ACCOUNT_FIELD,
            )
            accountErrorRes(state.errors)?.let { res ->
                Text(
                    text = stringResource(res),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (isTransfer) {
                DropdownField(
                    label = stringResource(R.string.template_destination_account),
                    options = accounts,
                    selected = accounts.firstOrNull { it.id == state.destinationAccountId },
                    optionLabel = { it.name },
                    onSelected = { onDestinationSelected(it.id) },
                    tag = TestTags.CATEGORY_FIELD,
                )
                destinationErrorRes(state.errors)?.let { res ->
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                DropdownField(
                    label = stringResource(R.string.template_category),
                    options = categories,
                    selected = categories.firstOrNull { it.id == state.categoryId },
                    optionLabel = { it.name },
                    onSelected = { onCategorySelected(it.id) },
                    tag = TestTags.CATEGORY_FIELD,
                )
                if (TemplateValidationError.CATEGORY_REQUIRED in state.errors) {
                    Text(
                        text = stringResource(R.string.error_template_category),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.template_default_amount)) },
                prefix = { Text(stringResource(R.string.currency_symbol)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = TemplateValidationError.INVALID_AMOUNT in state.errors,
                supportingText = if (TemplateValidationError.INVALID_AMOUNT in state.errors) {
                    { Text(stringResource(R.string.error_template_amount)) }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.TEMPLATE_AMOUNT_FIELD),
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.template_description)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.SAVE_TEMPLATE_BUTTON),
            ) {
                Text(stringResource(R.string.template_save))
            }
        }
    }
}

// el error de nombre puede ser vacio o duplicado
private fun nameErrorRes(errors: Set<TemplateValidationError>): Int? = when {
    TemplateValidationError.NAME_REQUIRED in errors -> R.string.error_template_name
    TemplateValidationError.DUPLICATE_NAME in errors -> R.string.error_template_duplicate
    else -> null
}

// la cuenta de origen solo muestra el error de cuenta requerida
private fun accountErrorRes(errors: Set<TemplateValidationError>): Int? =
    if (TemplateValidationError.ACCOUNT_REQUIRED in errors) R.string.error_template_account else null

// el destino comparte el mensaje de cuentas iguales
private fun destinationErrorRes(errors: Set<TemplateValidationError>): Int? = when {
    TemplateValidationError.SAME_ACCOUNTS in errors -> R.string.error_template_same_accounts
    else -> null
}
