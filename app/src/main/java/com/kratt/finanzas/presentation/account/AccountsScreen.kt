package com.kratt.finanzas.presentation.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.kratt.finanzas.presentation.common.listItemSpacing
import com.kratt.finanzas.presentation.common.maskedAmount

@Composable
fun AccountsRoute(
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onAccountClick: (Long) -> Unit,
    onAddTransfer: () -> Unit,
) {
    val viewModel = containerViewModel { AccountsViewModel(it.accountRepository, it.transactionRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AccountsScreen(state, onBack, onAddAccount, onAccountClick, onAddTransfer)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onAccountClick: (Long) -> Unit,
    onAddTransfer: () -> Unit = {},
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.ACCOUNTS_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.accounts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onAddTransfer, modifier = Modifier.testTag(TestTags.ADD_TRANSFER_BUTTON)) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = stringResource(R.string.transfer_label))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAccount,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_account)) },
                modifier = Modifier.testTag(TestTags.ADD_ACCOUNT_BUTTON),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(listItemSpacing()),
        ) {
            items(state.accounts, key = { it.account.id }) { row ->
                AccountCard(row, onClick = { onAccountClick(row.account.id) })
            }
        }
    }
}

@Composable
private fun AccountCard(row: AccountRowUi, onClick: () -> Unit) {
    val account = row.account
    val amountCents = if (row.balance.isCredit) row.balance.debtCents else row.balance.currentBalanceCents
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("${TestTags.ACCOUNT_ITEM}_${account.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardContentPadding()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(AccountTypeLabels.labelOf(account.type)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!account.isActive) {
                    Text(
                        text = stringResource(R.string.account_inactive_tag),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(if (row.balance.isCredit) R.string.balance_debt else R.string.balance_current),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // oculta el monto cuando la privacidad esta activa
                Text(text = maskedAmount(amountCents), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
