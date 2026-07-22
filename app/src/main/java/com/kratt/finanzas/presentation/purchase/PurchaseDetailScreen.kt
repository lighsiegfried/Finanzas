package com.kratt.finanzas.presentation.purchase

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.PurchaseStatus
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.common.maskedAmount
import com.kratt.finanzas.presentation.components.DateField
import com.kratt.finanzas.presentation.components.DropdownField
import java.time.LocalDate

@Composable
fun PurchaseDetailRoute(
    purchaseId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val viewModel = containerViewModel(key = "purchase_detail_$purchaseId") {
        PurchaseDetailViewModel(
            it.plannedPurchaseRepository,
            it.savingsGoalRepository,
            it.accountRepository,
            it.categoryRepository,
            it.planningReminderPreferences,
            it::rescheduleReminders,
            purchaseId,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val outcome by viewModel.registerOutcome.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // avisa el resultado del registro con un toast y limpia el estado
    LaunchedEffect(outcome) {
        when (outcome) {
            RegisterOutcome.SUCCESS -> {
                Toast.makeText(context, R.string.purchase_registered, Toast.LENGTH_SHORT).show()
                viewModel.onOutcomeConsumed()
            }
            RegisterOutcome.ERROR -> {
                Toast.makeText(context, R.string.error_purchase_register, Toast.LENGTH_SHORT).show()
                viewModel.onOutcomeConsumed()
            }
            RegisterOutcome.NONE -> Unit
        }
    }

    PurchaseDetailScreen(
        state = state,
        accounts = accounts,
        categories = categories,
        reminderEnabled = reminderEnabled,
        onToggleReminder = viewModel::onToggleReminder,
        onBack = onBack,
        onEdit = onEdit,
        onRegister = viewModel::onRegister,
        onReverse = viewModel::onReverse,
    )
}

// detalle de la compra con su avance y las acciones de registro o reversion
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    state: PurchaseDetailUiState,
    accounts: List<Account>,
    categories: List<Category>,
    reminderEnabled: Boolean = false,
    onToggleReminder: (Boolean) -> Unit = {},
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onRegister: (Long, Long, Long?, LocalDate, String?) -> Unit,
    onReverse: () -> Unit,
) {
    val purchase = state.purchase
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showReverseDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.testTag(TestTags.PURCHASE_DETAIL_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.purchases_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (purchase != null) {
                        IconButton(onClick = { onEdit(purchase.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.purchase_edit))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (purchase == null) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = purchase.name, style = MaterialTheme.typography.headlineSmall)

            DetailRow(
                label = stringResource(R.string.purchase_cost),
                value = maskedAmount(purchase.estimatedCostCents),
            )
            // la prioridad y el estado siempre en texto, nunca solo por color
            DetailRow(
                label = stringResource(R.string.purchase_priority),
                value = stringResource(priorityLabelRes(purchase.priority)),
            )
            DetailRow(
                label = stringResource(R.string.purchase_status_label),
                value = stringResource(purchaseStatusLabelRes(purchase.status)),
            )

            HorizontalDivider()

            // meta ligada y avance calculado desde ella
            DetailRow(
                label = stringResource(R.string.purchase_goal),
                value = state.linkedGoal?.name ?: stringResource(R.string.purchase_no_goal),
            )
            DetailRow(
                label = stringResource(R.string.purchase_available_saved),
                value = maskedAmount(state.readiness.availableSavedCents),
            )
            DetailRow(
                label = stringResource(R.string.purchase_remaining_to_save),
                value = maskedAmount(state.readiness.remainingToPurchaseCents),
            )
            Text(
                text = stringResource(readinessLabelRes(state.readiness.readiness)),
                style = MaterialTheme.typography.titleMedium,
            )

            HorizontalDivider()

            // recordatorio opcional de la compra, apagado por defecto
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.reminder_purchase), modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = reminderEnabled, onCheckedChange = onToggleReminder)
            }

            HorizontalDivider()

            // el estado decide si se puede registrar o revertir la compra
            if (purchase.status == PurchaseStatus.PURCHASED) {
                Button(
                    onClick = { showReverseDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.REVERSE_PURCHASE_BUTTON),
                ) {
                    Text(stringResource(R.string.purchase_reverse))
                }
            } else {
                Button(
                    onClick = { showRegisterDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.REGISTER_PURCHASE_BUTTON),
                ) {
                    Text(stringResource(R.string.purchase_register))
                }
            }
        }
    }

    if (showRegisterDialog && purchase != null) {
        RegisterPurchaseDialog(
            defaultAmountText = AmountParser.formatCents(purchase.estimatedCostCents),
            defaultDescription = purchase.name,
            defaultCategoryId = purchase.categoryId,
            accounts = accounts,
            categories = categories,
            onDismiss = { showRegisterDialog = false },
            onConfirm = { amountCents, accountId, categoryId, date, description ->
                onRegister(amountCents, accountId, categoryId, date, description)
                showRegisterDialog = false
            },
        )
    }

    if (showReverseDialog) {
        AlertDialog(
            onDismissRequest = { showReverseDialog = false },
            title = { Text(stringResource(R.string.purchase_reverse)) },
            text = { Text(stringResource(R.string.purchase_reverse_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReverse()
                        showReverseDialog = false
                    },
                    modifier = Modifier.testTag(TestTags.CONFIRM_REVERSE_PURCHASE_BUTTON),
                ) {
                    Text(stringResource(R.string.purchase_reverse))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReverseDialog = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }
}

// dialogo de confirmacion con los datos editables de la compra a registrar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterPurchaseDialog(
    defaultAmountText: String,
    defaultDescription: String,
    defaultCategoryId: Long?,
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Long?, LocalDate, String?) -> Unit,
) {
    var amountText by remember { mutableStateOf(defaultAmountText) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var categoryId by remember { mutableStateOf(defaultCategoryId) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var description by remember { mutableStateOf(defaultDescription) }
    var amountInvalid by remember { mutableStateOf(false) }
    var accountMissing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.purchase_register)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.isEmpty() || AmountParser.isPartialInput(it)) {
                            amountText = it
                            amountInvalid = false
                        }
                    },
                    label = { Text(stringResource(R.string.purchase_final_amount)) },
                    prefix = { Text(stringResource(R.string.currency_symbol)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountInvalid,
                    supportingText = if (amountInvalid) {
                        { Text(stringResource(R.string.error_purchase_cost)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                DropdownField(
                    label = stringResource(R.string.account_label),
                    options = accounts,
                    selected = accounts.firstOrNull { it.id == accountId },
                    optionLabel = { it.name },
                    onSelected = {
                        accountId = it.id
                        accountMissing = false
                    },
                    tag = TestTags.ACCOUNT_FIELD,
                )
                if (accountMissing) {
                    Text(
                        text = stringResource(R.string.error_purchase_register),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                DropdownField(
                    label = stringResource(R.string.category_label),
                    options = categories,
                    selected = categories.firstOrNull { it.id == categoryId },
                    optionLabel = { it.name },
                    onSelected = { categoryId = it.id },
                    tag = TestTags.CATEGORY_FIELD,
                )

                DateField(
                    date = date,
                    onDateSelected = { date = it },
                    label = stringResource(R.string.date_label),
                    tag = TestTags.DATE_FIELD,
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.purchase_description)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cents = AmountParser.parseToCents(amountText)
                    val selectedAccount = accountId
                    // validamos monto y cuenta antes de registrar
                    when {
                        cents == null -> amountInvalid = true
                        selectedAccount == null -> accountMissing = true
                        else -> onConfirm(cents, selectedAccount, categoryId, date, description.ifBlank { null })
                    }
                },
                modifier = Modifier.testTag(TestTags.CONFIRM_REGISTER_PURCHASE_BUTTON),
            ) {
                Text(stringResource(R.string.purchase_register))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        },
    )
}

// fila simple de etiqueta y valor para el detalle
@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
