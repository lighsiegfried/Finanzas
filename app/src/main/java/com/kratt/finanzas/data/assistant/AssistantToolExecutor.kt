package com.kratt.finanzas.data.assistant

import com.kratt.finanzas.data.backup.BackupPreferencesRepository
import com.kratt.finanzas.data.reminder.CommitmentService
import com.kratt.finanzas.data.repository.BudgetRepository
import com.kratt.finanzas.data.repository.InstallmentRepository
import com.kratt.finanzas.data.repository.PlannedPurchaseRepository
import com.kratt.finanzas.data.repository.RecurringRepository
import com.kratt.finanzas.data.repository.ReportRepository
import com.kratt.finanzas.data.repository.SavingsContributionRepository
import com.kratt.finanzas.data.repository.SavingsGoalRepository
import com.kratt.finanzas.domain.assistant.EntityCandidate
import com.kratt.finanzas.domain.assistant.GoalStatus
import com.kratt.finanzas.domain.assistant.InstallmentPlanStatus
import com.kratt.finanzas.domain.assistant.NamedBalance
import com.kratt.finanzas.domain.assistant.PlanContextData
import com.kratt.finanzas.domain.assistant.PurchaseStatusItem
import com.kratt.finanzas.domain.assistant.ToolRequest
import com.kratt.finanzas.domain.assistant.ToolResult
import com.kratt.finanzas.domain.model.AccountTotals
import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus
import com.kratt.finanzas.domain.model.InstallmentStatus
import com.kratt.finanzas.domain.model.PurchaseStatus
import com.kratt.finanzas.domain.model.SavingsGoalStatus
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.AccountBalanceCalculator
import com.kratt.finanzas.domain.usecase.AccountTotalsCalculator
import com.kratt.finanzas.domain.usecase.BudgetCalculator
import com.kratt.finanzas.domain.usecase.BudgetProgress
import com.kratt.finanzas.domain.usecase.Commitment
import com.kratt.finanzas.domain.usecase.CommittedTotals
import com.kratt.finanzas.domain.usecase.ContributionPoint
import com.kratt.finanzas.domain.usecase.DateRange
import com.kratt.finanzas.domain.usecase.GoalProgressCalculator
import com.kratt.finanzas.domain.usecase.InstallmentProgressCalculator
import com.kratt.finanzas.domain.usecase.MoneyMath
import com.kratt.finanzas.domain.usecase.MonthlyComparison
import com.kratt.finanzas.domain.usecase.ObserveMonthlySummaryUseCase
import com.kratt.finanzas.domain.usecase.OccurrenceView
import com.kratt.finanzas.domain.usecase.PlannedPurchaseReadinessCalculator
import com.kratt.finanzas.domain.usecase.TransactionFilter
import com.kratt.finanzas.domain.usecase.TransactionSearch
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first

// ejecuta las herramientas del asistente reutilizando el dominio; nunca hace sql libre ni escribe datos
interface AssistantToolExecutor {
    suspend fun execute(request: ToolRequest): ToolResult

    // candidatos activos para resolver cuentas, categorias, metas y compras por nombre
    suspend fun loadContext(): PlanContextData
}

class DefaultAssistantToolExecutor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val reportRepository: ReportRepository,
    private val budgetRepository: BudgetRepository,
    private val installmentRepository: InstallmentRepository,
    private val recurringRepository: RecurringRepository,
    private val commitmentService: CommitmentService,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val savingsContributionRepository: SavingsContributionRepository,
    private val plannedPurchaseRepository: PlannedPurchaseRepository,
    private val backupPreferencesRepository: BackupPreferencesRepository,
    private val observeMonthlySummary: ObserveMonthlySummaryUseCase,
    private val today: () -> LocalDate = LocalDate::now,
) : AssistantToolExecutor {

    override suspend fun execute(request: ToolRequest): ToolResult = try {
        runTool(request)
    } catch (error: ArithmeticException) {
        // un total desbordo el rango soportado, se reporta de forma segura
        ToolResult.AmountTooLarge
    }

    private suspend fun runTool(request: ToolRequest): ToolResult = when (request) {
        is ToolRequest.MonthlySummary ->
            ToolResult.MonthlySummaryResult(observeMonthlySummary(request.month).first())

        is ToolRequest.IncomeExpenseForPeriod ->
            ToolResult.IncomeExpenseResult(reportRepository.incomeExpense(request.period.range))

        is ToolRequest.ExpensesByCategory ->
            ToolResult.CategoryTotalsResult(reportRepository.expensesByCategory(request.period.range))

        is ToolRequest.ExpensesByAccount ->
            ToolResult.AccountTotalsResult(reportRepository.expensesByAccount(request.period.range))

        is ToolRequest.ComparePeriods -> {
            val current = reportRepository.incomeExpense(request.current.range)
            val previous = reportRepository.incomeExpense(request.previous.range)
            ToolResult.ComparisonResult(
                income = MonthlyComparison.compare(previous.incomeCents, current.incomeCents),
                expense = MonthlyComparison.compare(previous.expenseCents, current.expenseCents),
                net = MonthlyComparison.compare(previous.netCents, current.netCents),
            )
        }

        ToolRequest.AccountBalances -> {
            val accounts = accountRepository.observeActiveAccounts().first()
            val totals = AccountTotalsCalculator.totalsByAccount(transactionRepository.observeAllTransactions().first())
            ToolResult.AccountBalancesResult(
                accounts.map { account ->
                    NamedBalance(account.name, AccountBalanceCalculator.calculate(account, totals[account.id] ?: AccountTotals.EMPTY))
                },
            )
        }

        ToolRequest.CreditCardDebt ->
            ToolResult.CreditCardDebtResult(reportRepository.creditCardDebt().map { NamedBalance(it.first, it.second) })

        is ToolRequest.UpcomingPayments ->
            ToolResult.UpcomingPaymentsResult(commitmentService.dueCommitments(request.start, request.end))

        ToolRequest.InstallmentProgress -> {
            val plans = installmentRepository.observePlans().first().filter { it.status == InstallmentStatus.ACTIVE }
            ToolResult.InstallmentProgressResult(
                plans.map { plan ->
                    val occurrences = installmentRepository.occurrencesForPlan(plan.id)
                    val progress = InstallmentProgressCalculator.calculate(
                        occurrences.map { OccurrenceView(it.amountCents, it.status) },
                    )
                    val finishDate = occurrences
                        .filter { it.status == InstallmentOccurrenceStatus.PENDING || it.status == InstallmentOccurrenceStatus.OVERDUE }
                        .maxOfOrNull { it.dueDate }
                    InstallmentPlanStatus(plan.name, progress, finishDate)
                },
            )
        }

        is ToolRequest.RecurringCommitments ->
            ToolResult.RecurringCommitmentsResult(
                recurringRepository.occurrencesDueBetween(request.start, request.end)
                    .map { Commitment(it.scheduledDate, it.amountCents) },
            )

        is ToolRequest.BudgetStatus -> buildBudgetStatus(request.month)

        is ToolRequest.AvailableAfterCommitments -> buildAvailableAfterCommitments(request.month)

        is ToolRequest.SavingsGoalProgress -> buildSavingsGoalProgress(request.goalId)

        is ToolRequest.PlannedPurchaseReadiness -> buildPurchaseReadiness(request.purchaseId)

        is ToolRequest.SearchTransactions -> buildSearch(request)

        is ToolRequest.DetectPossibleDuplicates -> {
            val items = transactionRepository.observeAll().first()
            ToolResult.DuplicatesResult(com.kratt.finanzas.domain.assistant.DuplicateDetector.detect(items, request.windowDays))
        }

        ToolRequest.BackupStatus -> {
            val metadata = backupPreferencesRepository.metadata.first()
            ToolResult.BackupStatusResult(metadata.hasBackup, metadata.lastBackupMillis)
        }
    }

    private suspend fun buildBudgetStatus(month: YearMonth): ToolResult {
        val budgets = budgetRepository.observeForMonth(month.year, month.monthValue).first()
        if (budgets.isEmpty()) return ToolResult.BudgetStatusResult(null, emptyList())
        val range = DateRange(month.atDay(1), month.atEndOfMonth())
        val overallSpent = reportRepository.incomeExpense(range).expenseCents
        val categoryTotals = reportRepository.expensesByCategory(range).associate { it.id to it.totalCents }
        val categoryNames = categoryRepository.observeActiveByType(TransactionType.EXPENSE).first().associate { it.id to it.name }

        var overall: com.kratt.finanzas.domain.assistant.BudgetLine? = null
        val lines = mutableListOf<com.kratt.finanzas.domain.assistant.BudgetLine>()
        for (budget in budgets) {
            val categoryId = budget.categoryId
            if (categoryId == null) {
                overall = com.kratt.finanzas.domain.assistant.BudgetLine(
                    categoryName = null,
                    progress = progress(budget.limitAmountCents, overallSpent, budget.warningPercentage),
                )
            } else {
                val spent = categoryTotals[categoryId] ?: 0L
                lines += com.kratt.finanzas.domain.assistant.BudgetLine(
                    categoryName = categoryNames[categoryId],
                    progress = progress(budget.limitAmountCents, spent, budget.warningPercentage),
                )
            }
        }
        return ToolResult.BudgetStatusResult(overall, lines)
    }

    private fun progress(limit: Long, spent: Long, warning: Int): BudgetProgress =
        BudgetCalculator.progress(limit, spent, warning)

    private suspend fun buildAvailableAfterCommitments(month: YearMonth): ToolResult {
        val summary = observeMonthlySummary(month).first()
        val commitments = commitmentService.dueCommitments(month.atDay(1), month.atEndOfMonth())
        val committed = CommittedTotals.forMonth(commitments, month)
        // disponible estimado: ingresos menos gastos registrados menos compromisos pendientes
        val available = MoneyMath.subtract(MoneyMath.subtract(summary.incomeCents, summary.expenseCents), committed)
        return ToolResult.AvailableAfterCommitmentsResult(
            incomeCents = summary.incomeCents,
            expenseCents = summary.expenseCents,
            committedCents = committed,
            availableCents = available,
        )
    }

    private suspend fun buildSavingsGoalProgress(goalId: Long?): ToolResult {
        val all = savingsGoalRepository.observeAll().first().filter { !it.isArchived }
        val selected = if (goalId != null) {
            all.filter { it.id == goalId }
        } else {
            all.filter { it.status == SavingsGoalStatus.ACTIVE }
        }
        val goals = selected.map { goal ->
            val contributions = savingsContributionRepository.observeByGoal(goal.id).first()
                .map { ContributionPoint(it.amountCents, it.contributionDate) }
            GoalStatus(
                goalName = goal.name,
                targetCents = goal.targetAmountCents,
                progress = GoalProgressCalculator.calculate(goal.targetAmountCents, contributions, goal.startDate, goal.targetDate, today()),
            )
        }
        return ToolResult.SavingsGoalProgressResult(goals)
    }

    private suspend fun buildPurchaseReadiness(purchaseId: Long?): ToolResult {
        val all = plannedPurchaseRepository.observeAll().first()
        val active = if (purchaseId != null) {
            all.filter { it.id == purchaseId }
        } else {
            all.filter {
                it.status == PurchaseStatus.PLANNING || it.status == PurchaseStatus.SAVING || it.status == PurchaseStatus.READY
            }
        }
        val purchases = active.map { purchase ->
            val goalId = purchase.savingsGoalId
            val contributed = goalId?.let { savingsGoalRepository.totalByGoal(it) }
            val goalName = goalId?.let { savingsGoalRepository.findById(it)?.name }
            PurchaseStatusItem(
                purchaseName = purchase.name,
                estimatedCostCents = purchase.estimatedCostCents,
                readiness = PlannedPurchaseReadinessCalculator.calculate(purchase.status, purchase.estimatedCostCents, contributed),
                linkedGoalName = goalName,
            )
        }
        return ToolResult.PlannedPurchaseReadinessResult(purchases)
    }

    private suspend fun buildSearch(request: ToolRequest.SearchTransactions): ToolResult {
        val all = transactionRepository.observeAll().first()
        // si el usuario indico un periodo se respeta; si no, se busca en todo el historial
        val scoped = if (request.period.wasExplicit) {
            all.filter { !it.date.isBefore(request.period.range.start) && !it.date.isAfter(request.period.range.end) }
        } else {
            all
        }
        val filtered = TransactionSearch.apply(
            scoped,
            TransactionFilter(accountId = request.accountId, categoryId = request.categoryId, query = request.queryText),
        )
        return ToolResult.SearchResult(filtered)
    }

    override suspend fun loadContext(): PlanContextData {
        val accounts = accountRepository.observeActiveAccounts().first().map { EntityCandidate(it.id, it.name) }
        val expenseCategories = categoryRepository.observeActiveByType(TransactionType.EXPENSE).first()
        val incomeCategories = categoryRepository.observeActiveByType(TransactionType.INCOME).first()
        val categories = (expenseCategories + incomeCategories)
            .distinctBy { it.id }
            .map { EntityCandidate(it.id, it.name) }
        val goals = savingsGoalRepository.observeAll().first().filter { !it.isArchived }.map { EntityCandidate(it.id, it.name) }
        val purchases = plannedPurchaseRepository.observeAll().first().map { EntityCandidate(it.id, it.name) }
        return PlanContextData(accounts = accounts, categories = categories, goals = goals, purchases = purchases)
    }
}
