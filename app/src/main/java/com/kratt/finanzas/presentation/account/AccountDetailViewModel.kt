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
import kotlinx.coroutines.launch

data class AccountDetailUiState(
    val loaded: Boolean = false,
    val account: Account? = null,
    val balance: AccountBalance? = null,
)

class AccountDetailViewModel(
    private val accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    private val accountId: Long,
) : ViewModel() {

    // recalcula el saldo de la cuenta cada vez que cambian sus movimientos
    val uiState: StateFlow<AccountDetailUiState> = combine(
        accountRepository.observeAllAccounts(),
        transactionRepository.observeAllTransactions(),
    ) { accounts, transactions ->
        val account = accounts.firstOrNull { it.id == accountId }
        val balance = account?.let {
            val totals = AccountTotalsCalculator.totalsFor(accountId, transactions)
            AccountBalanceCalculator.calculate(it, totals)
        }
        AccountDetailUiState(loaded = true, account = account, balance = balance)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountDetailUiState())

    fun onToggleActive() {
        val account = uiState.value.account ?: return
        viewModelScope.launch {
            accountRepository.setActive(account.id, !account.isActive)
        }
    }
}
