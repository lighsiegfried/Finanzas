package com.kratt.finanzas.presentation.transfer

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
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.components.DateField
import com.kratt.finanzas.presentation.components.DropdownField

@Composable
fun TransferFormRoute(transferId: Long?, onDone: () -> Unit, templateId: Long? = null) {
    val viewModel = containerViewModel(key = "transfer_form_${transferId}_${templateId}") {
        TransferFormViewModel(it.accountRepository, it.transactionRepository, it.saveTransfer, transferId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val container = (appContext.applicationContext as com.kratt.finanzas.FinanzasApplication).container
    // usa una plantilla de transferencia: prellena origen, destino y monto
    LaunchedEffect(Unit) {
        if (templateId != null) {
            val template = container.transactionTemplateRepository.findById(templateId)
            if (template != null && template.type == com.kratt.finanzas.domain.model.TransactionType.TRANSFER) {
                viewModel.onSourceSelected(template.accountId)
                template.destinationAccountId?.let { viewModel.onDestinationSelected(it) }
                template.defaultAmountCents?.let { viewModel.onAmountChange(com.kratt.finanzas.common.AmountParser.formatCents(it)) }
                template.description?.let { viewModel.onDescriptionChange(it) }
                container.transactionTemplateRepository.markUsed(templateId)
            }
        }
    }
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            container.refreshWidgets()
            android.widget.Toast.makeText(appContext, appContext.getString(com.kratt.finanzas.R.string.saved_transfer), android.widget.Toast.LENGTH_SHORT).show()
            onDone()
        }
    }
    TransferFormScreen(
        state = state,
        onBack = onDone,
        onSourceSelected = viewModel::onSourceSelected,
        onDestinationSelected = viewModel::onDestinationSelected,
        onAmountChange = viewModel::onAmountChange,
        onDateSelected = viewModel::onDateSelected,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSave = viewModel::onSave,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFormScreen(
    state: TransferFormUiState,
    onBack: () -> Unit,
    onSourceSelected: (Long) -> Unit,
    onDestinationSelected: (Long) -> Unit,
    onAmountChange: (String) -> Unit,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.TRANSFER_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.transfer_label)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val accountLabel: @Composable (Account) -> String = { it.name }
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DropdownField(
                label = stringResource(R.string.transfer_source_label),
                options = state.accounts,
                selected = state.accounts.firstOrNull { it.id == state.sourceId },
                optionLabel = accountLabel,
                onSelected = { onSourceSelected(it.id) },
                tag = TestTags.TRANSFER_SOURCE_FIELD,
            )
            state.sourceErrorRes?.let { ErrorText(it) }
            DropdownField(
                label = stringResource(R.string.transfer_destination_label),
                options = state.accounts,
                selected = state.accounts.firstOrNull { it.id == state.destinationId },
                optionLabel = accountLabel,
                onSelected = { onDestinationSelected(it.id) },
                tag = TestTags.TRANSFER_DESTINATION_FIELD,
            )
            state.destinationErrorRes?.let { ErrorText(it) }
            OutlinedTextField(
                value = state.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.amount_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.amountErrorRes != null,
                supportingText = state.amountErrorRes?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.TRANSFER_AMOUNT_FIELD),
            )
            DateField(
                date = state.date,
                onDateSelected = onDateSelected,
                label = stringResource(R.string.date_label),
                tag = "transfer_date_field",
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.description_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.SAVE_TRANSFER_BUTTON),
            ) {
                Text(stringResource(R.string.save_transfer))
            }
        }
    }
}

@Composable
private fun ErrorText(res: Int) {
    Text(text = stringResource(res), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}
