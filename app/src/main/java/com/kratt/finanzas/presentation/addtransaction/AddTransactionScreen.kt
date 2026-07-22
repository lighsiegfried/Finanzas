package com.kratt.finanzas.presentation.addtransaction

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kratt.finanzas.R
import com.kratt.finanzas.common.ShortDateFormatter
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.di.AppViewModelProvider
import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate

// milisegundos que tiene un dia, para hablar con el selector de fecha
private const val MILLIS_PER_DAY = 86_400_000L

@Composable
fun AddTransactionRoute(
    onNavigateBack: () -> Unit,
    initialType: TransactionType? = null,
    templateId: Long? = null,
    viewModel: AddTransactionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val container = (appContext.applicationContext as com.kratt.finanzas.FinanzasApplication).container
    // preselecciona el tipo cuando la accion rapida lo indica (gasto o ingreso)
    LaunchedEffect(Unit) {
        if (initialType != null) viewModel.onTypeChange(initialType)
        // usa una plantilla: prellena el formulario normal y marca la plantilla como usada
        if (templateId != null) {
            val template = container.transactionTemplateRepository.findById(templateId)
            if (template != null) {
                viewModel.onTypeChange(template.type)
                viewModel.onAccountSelected(template.accountId)
                template.categoryId?.let { viewModel.onCategorySelected(it) }
                template.defaultAmountCents?.let { viewModel.onAmountChange(com.kratt.finanzas.common.AmountParser.formatCents(it)) }
                template.description?.let { viewModel.onDescriptionChange(it) }
                container.transactionTemplateRepository.markUsed(templateId)
            }
        }
    }
    // regresa al resumen cuando el movimiento ya quedo guardado, avisa y refresca los widgets
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            container.refreshWidgets()
            val msgRes = when (state.type) {
                TransactionType.INCOME -> com.kratt.finanzas.R.string.saved_income
                else -> com.kratt.finanzas.R.string.saved_expense
            }
            android.widget.Toast.makeText(appContext, appContext.getString(msgRes), android.widget.Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }
    AddTransactionScreen(
        state = state,
        onTypeChange = viewModel::onTypeChange,
        onAmountChange = viewModel::onAmountChange,
        onAccountSelected = viewModel::onAccountSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onDescriptionChange = viewModel::onDescriptionChange,
        onDateSelected = viewModel::onDateSelected,
        onSaveClick = viewModel::onSaveClick,
        onCancelClick = onNavigateBack,
        onSaveErrorShown = viewModel::onSaveErrorShown,
    )
}

// formulario para registrar un gasto o un ingreso
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    state: AddTransactionUiState,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onAccountSelected: (Long) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSaveErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    // muestra el error de guardado como aviso pasajero
    LaunchedEffect(state.saveErrorRes) {
        state.saveErrorRes?.let { res ->
            snackbarHostState.showSnackbar(context.getString(res))
            onSaveErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.testTag(TestTags.ADD_TRANSACTION_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.add_transaction)) },
                navigationIcon = {
                    IconButton(onClick = onCancelClick) {
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
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { onTypeChange(TransactionType.EXPENSE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag(TestTags.TYPE_EXPENSE_OPTION),
                ) {
                    Text(stringResource(R.string.transaction_type_expense))
                }
                SegmentedButton(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { onTypeChange(TransactionType.INCOME) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag(TestTags.TYPE_INCOME_OPTION),
                ) {
                    Text(stringResource(R.string.transaction_type_income))
                }
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.amount_label)) },
                prefix = { Text(stringResource(R.string.currency_symbol)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.amountErrorRes != null,
                supportingText = state.amountErrorRes?.let { res -> { Text(stringResource(res)) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.AMOUNT_FIELD),
            )

            SelectorField(
                label = stringResource(R.string.account_label),
                selectedName = state.accounts
                    .firstOrNull { it.id == state.selectedAccountId }?.name.orEmpty(),
                options = state.accounts.map { SelectorOption(it.id, it.name) },
                onSelected = onAccountSelected,
                errorRes = state.accountErrorRes,
                tag = TestTags.ACCOUNT_FIELD,
            )

            SelectorField(
                label = stringResource(R.string.category_label),
                selectedName = state.categories
                    .firstOrNull { it.id == state.selectedCategoryId }?.name.orEmpty(),
                options = state.categories.map { SelectorOption(it.id, it.name) },
                onSelected = onCategorySelected,
                errorRes = state.categoryErrorRes,
                tag = TestTags.CATEGORY_FIELD,
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.description_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.DESCRIPTION_FIELD),
            )

            OutlinedTextField(
                value = ShortDateFormatter.format(state.date),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.date_label)) },
                trailingIcon = {
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.testTag(TestTags.DATE_PICKER_BUTTON),
                    ) {
                        Icon(
                            Icons.Filled.DateRange,
                            contentDescription = stringResource(R.string.select_date),
                        )
                    }
                },
                isError = state.dateErrorRes != null,
                supportingText = state.dateErrorRes?.let { res -> { Text(stringResource(res)) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.DATE_FIELD),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.CANCEL_BUTTON),
                ) {
                    Text(stringResource(R.string.cancel_action))
                }
                Button(
                    onClick = onSaveClick,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.SAVE_BUTTON),
                ) {
                    Text(stringResource(R.string.save_transaction))
                }
            }
        }
    }

    if (showDatePicker) {
        // el selector de material trabaja con milisegundos utc
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.toEpochDay() * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(LocalDate.ofEpochDay(millis / MILLIS_PER_DAY))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.accept_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private data class SelectorOption(val id: Long, val name: String)

// campo de seleccion con menu desplegable para cuenta y categoria
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorField(
    label: String,
    selectedName: String,
    options: List<SelectorOption>,
    onSelected: (Long) -> Unit,
    errorRes: Int?,
    tag: String,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = errorRes != null,
            supportingText = errorRes?.let { res -> { Text(stringResource(res)) } },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag(tag),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        onSelected(option.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
