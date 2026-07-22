package com.kratt.finanzas.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.presentation.common.AccountTypeLabels
import com.kratt.finanzas.presentation.common.cardContentPadding
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.common.maskedAmount

@Composable
fun AccountDetailRoute(accountId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val viewModel = containerViewModel(key = "account_detail_$accountId") {
        AccountDetailViewModel(it.accountRepository, it.transactionRepository, accountId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AccountDetailScreen(state, onBack, onEdit = { onEdit(accountId) }, onToggleActive = viewModel::onToggleActive)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    state: AccountDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
) {
    var showDeactivate by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.testTag(TestTags.ACCOUNT_DETAIL_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(state.account?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag(TestTags.EDIT_ACCOUNT_BUTTON)) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_action))
                    }
                },
            )
        },
    ) { innerPadding ->
        val account = state.account
        val balance = state.balance
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (account != null && balance != null) {
                Text(
                    text = stringResource(AccountTypeLabels.labelOf(account.type)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    // oculta los montos cuando la privacidad esta activa
                    Column(modifier = Modifier.padding(cardContentPadding()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (balance.isCredit) {
                            DetailRow(stringResource(R.string.balance_debt), maskedAmount(balance.debtCents))
                            DetailRow(
                                stringResource(R.string.credit_limit_label),
                                if (balance.hasCreditLimit) maskedAmount(balance.creditLimitCents) else stringResource(R.string.no_credit_limit),
                            )
                            DetailRow(
                                stringResource(R.string.balance_available),
                                balance.availableCreditCents?.let { maskedAmount(it) } ?: stringResource(R.string.no_credit_limit),
                            )
                        } else {
                            DetailRow(stringResource(R.string.balance_current), maskedAmount(balance.currentBalanceCents))
                            DetailRow(stringResource(R.string.balance_initial), maskedAmount(account.initialBalanceCents))
                            DetailRow(stringResource(R.string.balance_available), maskedAmount(balance.currentBalanceCents))
                        }
                    }
                }
                OutlinedButton(
                    onClick = { if (account.isActive) showDeactivate = true else onToggleActive() },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.DEACTIVATE_ACCOUNT_BUTTON),
                ) {
                    Text(stringResource(if (account.isActive) R.string.deactivate_account else R.string.reactivate_account))
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

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}
