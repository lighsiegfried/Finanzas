package com.kratt.finanzas.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.common.MonthFormatter
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionListItem
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.MonthNavigator
import com.kratt.finanzas.domain.usecase.TransactionFilter
import com.kratt.finanzas.domain.usecase.TransactionSearch
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val monthLabel: String = "",
    val isCurrentMonth: Boolean = true,
    val query: String = "",
    val filter: TransactionFilter = TransactionFilter(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val results: List<TransactionListItem> = emptyList(),
    val hasMovementsThisMonth: Boolean = false,
)

private data class MonthItems(val month: YearMonth, val items: List<TransactionListItem>)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    initialType: TransactionType? = null,
    initialAccountId: Long? = null,
    initialCategoryId: Long? = null,
    private val currentMonth: YearMonth = YearMonth.now(),
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(currentMonth)
    private val query = MutableStateFlow("")
    // filtros iniciales cuando el historial se abre desde una grafica
    private val typeFilter = MutableStateFlow(initialType)
    private val accountFilter = MutableStateFlow(initialAccountId)
    private val categoryFilter = MutableStateFlow(initialCategoryId)

    private val monthItems = selectedMonth.flatMapLatest { month ->
        transactionRepository.observeMonthlyWithNames(month).map { MonthItems(month, it) }
    }

    private val filters = combine(query, typeFilter, accountFilter, categoryFilter) { q, type, account, category ->
        TransactionFilter(type = type, accountId = account, categoryId = category, query = q)
    }

    // categorias activas de ambos tipos para el filtro
    private val activeCategories = combine(
        categoryRepository.observeActiveByType(TransactionType.EXPENSE),
        categoryRepository.observeActiveByType(TransactionType.INCOME),
    ) { expense, income -> expense + income }

    val uiState: StateFlow<TransactionsUiState> = combine(
        monthItems,
        filters,
        accountRepository.observeActiveAccounts(),
        activeCategories,
    ) { month, filter, accounts, categories ->
        TransactionsUiState(
            isLoading = false,
            monthLabel = MonthFormatter.format(month.month),
            isCurrentMonth = MonthNavigator.isCurrent(month.month, currentMonth),
            query = filter.query,
            filter = filter,
            accounts = accounts,
            categories = categories,
            results = TransactionSearch.apply(month.items, filter),
            hasMovementsThisMonth = month.items.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun onQueryChange(value: String) { query.value = value }
    fun onTypeFilter(type: TransactionType?) { typeFilter.value = type }
    fun onAccountFilter(id: Long?) { accountFilter.value = id }
    fun onCategoryFilter(id: Long?) { categoryFilter.value = id }
    fun onPreviousMonth() { selectedMonth.value = MonthNavigator.previous(selectedMonth.value) }
    fun onNextMonth() { selectedMonth.value = MonthNavigator.next(selectedMonth.value) }
    fun onCurrentMonth() { selectedMonth.value = currentMonth }

    fun onClearFilters() {
        query.value = ""
        typeFilter.value = null
        accountFilter.value = null
        categoryFilter.value = null
    }
}
