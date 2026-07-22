package com.kratt.finanzas.presentation.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.common.MonthFormatter
import com.kratt.finanzas.data.preferences.DisplayPreferences
import com.kratt.finanzas.data.reminder.CommitmentService
import com.kratt.finanzas.data.repository.BudgetRepository
import com.kratt.finanzas.data.repository.PlannedPurchaseRepository
import com.kratt.finanzas.data.repository.ReportRepository
import com.kratt.finanzas.data.repository.SavingsGoalRepository
import com.kratt.finanzas.domain.model.AccountTotals
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.DashboardModule
import com.kratt.finanzas.domain.model.PurchasePriority
import com.kratt.finanzas.domain.model.PurchaseStatus
import com.kratt.finanzas.domain.model.QuickAction
import com.kratt.finanzas.domain.model.SavingsGoalStatus
import com.kratt.finanzas.domain.model.TransactionListItem
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.AccountBalanceCalculator
import com.kratt.finanzas.domain.usecase.AccountTotalsCalculator
import com.kratt.finanzas.domain.usecase.BudgetCalculator
import com.kratt.finanzas.domain.usecase.BudgetState
import com.kratt.finanzas.domain.usecase.PlannedPurchaseReadinessCalculator
import com.kratt.finanzas.domain.usecase.CommittedTotals
import com.kratt.finanzas.domain.usecase.DateRange
import com.kratt.finanzas.domain.usecase.DueClassifier
import com.kratt.finanzas.domain.usecase.DueWhen
import com.kratt.finanzas.domain.usecase.MonthNavigator
import com.kratt.finanzas.domain.usecase.MonthRange
import com.kratt.finanzas.domain.usecase.ObserveMonthlySummaryUseCase
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val RECENT_LIMIT = 15
private const val UPCOMING_LIMIT = 5
private const val TOP_CATEGORIES = 5
private const val TREND_MONTHS = 6

data class UpcomingItem(
    val dueDate: LocalDate,
    val amountCents: Long,
    val dueWhen: DueWhen,
)

// saldo de una cuenta para el modulo de saldos, ahorros o deuda de tarjetas
data class AccountBalanceItem(
    val name: String,
    val amountCents: Long,
)

data class CategoryTotalItem(
    val name: String,
    val totalCents: Long,
)

data class TrendItem(
    val label: String,
    val balanceCents: Long,
)

// progreso del presupuesto general del mes; solo cuenta gastos registrados
data class BudgetProgressItem(
    val limitCents: Long,
    val spentCents: Long,
    val remainingCents: Long,
    val state: BudgetState,
)

// avance de una meta para el modulo del resumen
data class GoalSummaryItem(
    val name: String,
    val contributedCents: Long,
    val targetCents: Long,
    val progressPercent: Int,
    val targetDate: LocalDate?,
)

// compra planificada para el modulo del resumen
data class PurchaseSummaryItem(
    val name: String,
    val estimatedCostCents: Long,
    val priority: PurchasePriority,
    val remainingCents: Long,
    val targetDate: LocalDate?,
)

data class SummaryUiState(
    val isLoading: Boolean = true,
    val monthLabel: String = "",
    val isCurrentMonth: Boolean = true,
    val incomeCents: Long = 0L,
    val expenseCents: Long = 0L,
    val balanceCents: Long = 0L,
    val recentTransactions: List<TransactionListItem> = emptyList(),
    val committedCents: Long = 0L,
    val upcoming: List<UpcomingItem> = emptyList(),
    val quickActions: List<QuickAction> = QuickAction.DEFAULTS,
    val accountBalances: List<AccountBalanceItem> = emptyList(),
    val creditCards: List<AccountBalanceItem> = emptyList(),
    val savingsAccounts: List<AccountBalanceItem> = emptyList(),
    val savingsTotalCents: Long = 0L,
    val expenseCategories: List<CategoryTotalItem> = emptyList(),
    val trend: List<TrendItem> = emptyList(),
    val overallBudget: BudgetProgressItem? = null,
    val savingsGoals: List<GoalSummaryItem> = emptyList(),
    val closestGoal: GoalSummaryItem? = null,
    val plannedPurchases: List<PurchaseSummaryItem> = emptyList(),
    val nextPurchase: PurchaseSummaryItem? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModel(
    private val observeMonthlySummary: ObserveMonthlySummaryUseCase,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val reportRepository: ReportRepository,
    private val budgetRepository: BudgetRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val plannedPurchaseRepository: PlannedPurchaseRepository,
    private val commitmentService: CommitmentService,
    private val displayPreferences: DisplayPreferences,
    private val currentMonth: YearMonth = YearMonth.now(),
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(currentMonth)

    // privacidad de saldos: refleja la preferencia y su interruptor rapido en el resumen
    val balancesHidden: StateFlow<Boolean> = displayPreferences.settings
        .map { it.balancesHidden }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onToggleBalances() {
        viewModelScope.launch { displayPreferences.setBalancesHidden(!balancesHidden.value) }
    }

    // modulos opcionales visibles del resumen en el orden elegido por el usuario
    val dashboardModules: StateFlow<List<DashboardModule>> = displayPreferences.settings
        .map { settings -> settings.dashboardOrder.filter { it !in settings.hiddenModules } }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            DashboardModule.DEFAULT_ORDER.filter { it !in DashboardModule.DEFAULT_HIDDEN },
        )

    // reacciona al mes elegido y a las preferencias; arma los totales y los datos de cada modulo
    val uiState: StateFlow<SummaryUiState> = selectedMonth.flatMapLatest { month ->
        combine(
            observeMonthlySummary(month),
            transactionRepository.observeMonthlyWithNames(month),
            accountRepository.observeAllAccounts(),
            transactionRepository.observeAllTransactions(),
            displayPreferences.settings,
        ) { summary, items, accounts, allTx, settings ->
            val visible = settings.dashboardOrder.filter { it !in settings.hiddenModules }.toSet()
            val range = MonthRange.of(month)
            val dateRange = DateRange(
                LocalDate.ofEpochDay(range.startEpochDay),
                LocalDate.ofEpochDay(range.endEpochDay),
            )
            val commitments = commitmentService.dueCommitments(dateRange.start, dateRange.end)

            // saldos reales por cuenta con enteros; sirven para saldos, ahorros y deuda de tarjetas
            val totals = AccountTotalsCalculator.totalsByAccount(allTx)
            val activeAccounts = accounts.filter { it.isActive }
            val balances = activeAccounts.map { account ->
                account to AccountBalanceCalculator.calculate(account, totals[account.id] ?: AccountTotals.EMPTY)
            }
            val accountBalances = balances.map { (a, b) -> AccountBalanceItem(a.name, b.currentBalanceCents) }
            val creditCards = balances.filter { it.first.type == AccountType.CREDIT_CARD }
                .map { (a, b) -> AccountBalanceItem(a.name, b.debtCents) }
            val savings = balances.filter { it.first.type == AccountType.SAVINGS }
                .map { (a, b) -> AccountBalanceItem(a.name, b.currentBalanceCents) }

            // los reportes analiticos solo se consultan cuando su modulo esta visible
            val categories = if (DashboardModule.EXPENSE_CATEGORIES in visible) {
                reportRepository.expensesByCategory(dateRange).take(TOP_CATEGORIES)
                    .map { CategoryTotalItem(it.name, it.totalCents) }
            } else emptyList()
            val trend = if (DashboardModule.MONTHLY_TREND in visible) {
                reportRepository.monthlyTrend(month, TREND_MONTHS)
                    .map { TrendItem(MonthFormatter.format(YearMonth.of(it.year, it.month)), it.balanceCents) }
            } else emptyList()
            val overallBudget = if (DashboardModule.BUDGET_PROGRESS in visible) {
                budgetRepository.observeForMonth(month.year, month.monthValue).first()
                    .firstOrNull { it.isOverall }?.let { budget ->
                        val spent = reportRepository.incomeExpense(dateRange).expenseCents
                        val progress = BudgetCalculator.progress(budget.limitAmountCents, spent, budget.warningPercentage)
                        BudgetProgressItem(budget.limitAmountCents, spent, progress.remainingCents, progress.state)
                    }
            } else null

            // total ahorrado por meta, calculado con sql; se reusa para metas y compras
            val goalTotals = if (DashboardModule.SAVINGS_GOALS in visible || DashboardModule.PLANNED_PURCHASES in visible) {
                savingsGoalRepository.observeTotalsByGoal().first().associate { it.savingsGoalId to it.totalCents }
            } else emptyMap()

            val savingsGoals = if (DashboardModule.SAVINGS_GOALS in visible) {
                savingsGoalRepository.observeAll().first()
                    .filter { it.status == SavingsGoalStatus.ACTIVE && !it.isArchived }
                    .map { g ->
                        val contributed = goalTotals[g.id] ?: 0L
                        val pct = if (g.targetAmountCents > 0) ((contributed * 100) / g.targetAmountCents).toInt() else 0
                        GoalSummaryItem(g.name, contributed, g.targetAmountCents, pct, g.targetDate)
                    }
            } else emptyList()
            // meta mas cercana: la de fecha objetivo mas proxima que aun no ha pasado
            val closestGoal = savingsGoals.filter { it.targetDate != null && !it.targetDate.isBefore(today) }
                .minByOrNull { it.targetDate!! }

            val plannedPurchases = if (DashboardModule.PLANNED_PURCHASES in visible) {
                plannedPurchaseRepository.observeAll().first()
                    .filter { it.status != PurchaseStatus.PURCHASED && it.status != PurchaseStatus.CANCELLED && it.status != PurchaseStatus.ARCHIVED }
                    .map { p ->
                        val available = p.savingsGoalId?.let { goalTotals[it] }
                        val readiness = PlannedPurchaseReadinessCalculator.calculate(p.status, p.estimatedCostCents, available)
                        PurchaseSummaryItem(p.name, p.estimatedCostCents, p.priority, readiness.remainingToPurchaseCents, p.targetDate)
                    }
            } else emptyList()
            val nextPurchase = plannedPurchases.filter { it.targetDate != null && !it.targetDate.isBefore(today) }
                .minByOrNull { it.targetDate!! }

            SummaryUiState(
                isLoading = false,
                monthLabel = MonthFormatter.format(month),
                isCurrentMonth = MonthNavigator.isCurrent(month, currentMonth),
                incomeCents = summary.incomeCents,
                expenseCents = summary.expenseCents,
                balanceCents = summary.balanceCents,
                recentTransactions = items.take(RECENT_LIMIT),
                committedCents = CommittedTotals.forMonth(commitments, month),
                upcoming = commitments.sortedBy { it.dueDate }.take(UPCOMING_LIMIT).map {
                    UpcomingItem(it.dueDate, it.amountCents, DueClassifier.classify(it.dueDate, today))
                },
                quickActions = settings.quickActions,
                accountBalances = accountBalances,
                creditCards = creditCards,
                savingsAccounts = savings,
                savingsTotalCents = savings.sumOf { it.amountCents },
                expenseCategories = categories,
                trend = trend,
                overallBudget = overallBudget,
                savingsGoals = savingsGoals,
                closestGoal = closestGoal,
                plannedPurchases = plannedPurchases,
                nextPurchase = nextPurchase,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SummaryUiState(monthLabel = MonthFormatter.format(currentMonth)),
    )

    fun onPreviousMonth() { selectedMonth.value = MonthNavigator.previous(selectedMonth.value) }
    fun onNextMonth() { selectedMonth.value = MonthNavigator.next(selectedMonth.value) }
    fun onCurrentMonth() { selectedMonth.value = currentMonth }
}
