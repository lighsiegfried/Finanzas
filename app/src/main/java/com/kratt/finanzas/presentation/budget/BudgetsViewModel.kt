package com.kratt.finanzas.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.common.MonthFormatter
import com.kratt.finanzas.data.reminder.CommitmentService
import com.kratt.finanzas.data.repository.BudgetRepository
import com.kratt.finanzas.domain.model.Budget
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.BudgetCalculator
import com.kratt.finanzas.domain.usecase.BudgetProgress
import com.kratt.finanzas.domain.usecase.BudgetSpentCalculator
import com.kratt.finanzas.domain.usecase.BudgetState
import com.kratt.finanzas.domain.usecase.CommittedTotals
import com.kratt.finanzas.domain.usecase.MonthNavigator
import com.kratt.finanzas.domain.usecase.MonthRange
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class BudgetBanner { NEAR_LIMIT, CATEGORY_EXCEEDED, OVERALL_EXCEEDED }

data class BudgetRowUi(
    val budget: Budget,
    val categoryName: String?,
    val progress: BudgetProgress,
)

data class BudgetsUiState(
    val monthLabel: String = "",
    val isCurrentMonth: Boolean = true,
    val overall: BudgetRowUi? = null,
    val categoryBudgets: List<BudgetRowUi> = emptyList(),
    val actualExpensesCents: Long = 0L,
    val committedCents: Long = 0L,
    val availableCents: Long? = null,
    val categoriesWithoutBudget: List<Category> = emptyList(),
    val banner: BudgetBanner? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val commitmentService: CommitmentService,
    private val currentMonth: YearMonth = YearMonth.now(),
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(currentMonth)

    val uiState: StateFlow<BudgetsUiState> = selectedMonth.flatMapLatest { month ->
        combine(
            budgetRepository.observeForMonth(month.year, month.monthValue),
            transactionRepository.observeMonthly(month),
            categoryRepository.observeActiveByType(com.kratt.finanzas.domain.model.TransactionType.EXPENSE),
        ) { budgets, transactions, categories ->
            val names = categories.associate { it.id to it.name }
            val actualExpenses = BudgetSpentCalculator.spent(transactions, null)

            val overallBudget = budgets.firstOrNull { it.isOverall }
            val overallRow = overallBudget?.let {
                BudgetRowUi(it, null, BudgetCalculator.progress(it.limitAmountCents, actualExpenses, it.warningPercentage))
            }

            val categoryRows = budgets.filter { !it.isOverall }.map { budget ->
                val spent = BudgetSpentCalculator.spent(transactions, budget.categoryId)
                BudgetRowUi(budget, names[budget.categoryId], BudgetCalculator.progress(budget.limitAmountCents, spent, budget.warningPercentage))
            }

            val budgetedCategoryIds = budgets.mapNotNull { it.categoryId }.toSet()
            val withoutBudget = categories.filter { it.id !in budgetedCategoryIds }

            val range = MonthRange.of(month)
            val committed = CommittedTotals.total(
                commitmentService.dueCommitments(LocalDate.ofEpochDay(range.startEpochDay), LocalDate.ofEpochDay(range.endEpochDay)),
            )

            BudgetsUiState(
                monthLabel = MonthFormatter.format(month),
                isCurrentMonth = MonthNavigator.isCurrent(month, currentMonth),
                overall = overallRow,
                categoryBudgets = categoryRows,
                actualExpensesCents = actualExpenses,
                committedCents = committed,
                availableCents = overallRow?.progress?.remainingCents,
                categoriesWithoutBudget = withoutBudget,
                banner = computeBanner(overallRow, categoryRows),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    // elige el aviso mas severo para mostrar el banner
    private fun computeBanner(overall: BudgetRowUi?, categories: List<BudgetRowUi>): BudgetBanner? = when {
        overall?.progress?.state == BudgetState.EXCEEDED -> BudgetBanner.OVERALL_EXCEEDED
        categories.any { it.progress.state == BudgetState.EXCEEDED } -> BudgetBanner.CATEGORY_EXCEEDED
        overall?.progress?.state == BudgetState.WARNING || categories.any { it.progress.state == BudgetState.WARNING } -> BudgetBanner.NEAR_LIMIT
        else -> null
    }

    fun onPreviousMonth() { selectedMonth.value = MonthNavigator.previous(selectedMonth.value) }
    fun onNextMonth() { selectedMonth.value = MonthNavigator.next(selectedMonth.value) }
    fun onCurrentMonth() { selectedMonth.value = currentMonth }
    fun selectedMonth(): YearMonth = selectedMonth.value
}
