package com.kratt.finanzas.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.AccountBalance
import com.kratt.finanzas.domain.model.AccountTotals
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.AccountBalanceCalculator
import com.kratt.finanzas.domain.usecase.AccountTotalsCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AccountRowUi(
    val account: Account,
    val balance: AccountBalance,
)

data class AccountsUiState(
    val isLoading: Boolean = true,
    val accounts: List<AccountRowUi> = emptyList(),
)

class AccountsViewModel(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    // combina las cuentas con todos los movimientos para calcular sus saldos
    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.observeAllAccounts(),
        transactionRepository.observeAllTransactions(),
    ) { accounts, transactions ->
        val totals = AccountTotalsCalculator.totalsByAccount(transactions)
        AccountsUiState(
            isLoading = false,
            accounts = accounts.map { account ->
                AccountRowUi(
                    account = account,
                    balance = AccountBalanceCalculator.calculate(account, totals[account.id] ?: AccountTotals.EMPTY),
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())
}
