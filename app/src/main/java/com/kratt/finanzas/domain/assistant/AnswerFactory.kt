package com.kratt.finanzas.domain.assistant

import com.kratt.finanzas.domain.usecase.MoneyMath
import java.time.LocalDate

// arma la respuesta tipada a partir del plan y el resultado de la herramienta, de forma pura
object AnswerFactory {

    fun build(run: AssistantPlan.Run, result: ToolResult, today: LocalDate): AssistantAnswer {
        return when (result) {
            is ToolResult.AmountTooLarge -> AssistantAnswer.AmountTooLargeAnswer

            is ToolResult.MonthlySummaryResult -> {
                val month = (run.request as ToolRequest.MonthlySummary).month
                AssistantAnswer.MonthlySummaryAnswer(
                    period = PeriodLabel.Month(month.year, month.monthValue),
                    incomeCents = result.summary.incomeCents,
                    expenseCents = result.summary.expenseCents,
                    balanceCents = result.summary.balanceCents,
                )
            }

            is ToolResult.IncomeExpenseResult -> {
                val period = (run.request as ToolRequest.IncomeExpenseForPeriod).period
                AssistantAnswer.MonthlySummaryAnswer(
                    period = period.label,
                    incomeCents = result.value.incomeCents,
                    expenseCents = result.value.expenseCents,
                    balanceCents = result.value.netCents,
                )
            }

            is ToolResult.CategoryTotalsResult -> {
                val period = (run.request as ToolRequest.ExpensesByCategory).period.label
                val focus = run.focusCategory
                if (focus != null) {
                    val item = result.items.firstOrNull { it.id == focus.id }
                        ?: com.kratt.finanzas.domain.model.LabeledTotal(focus.id, focus.name, 0L, 0)
                    AssistantAnswer.ExpensesByCategoryAnswer(
                        period = period,
                        items = listOf(item),
                        action = AssistantAction.ViewMovements(categoryId = focus.id),
                    )
                } else {
                    val sorted = result.items.sortedByDescending { it.totalCents }
                    AssistantAnswer.ExpensesByCategoryAnswer(
                        period = period,
                        items = sorted,
                        action = sorted.firstOrNull()?.let { AssistantAction.ViewMovements(categoryId = it.id) },
                    )
                }
            }

            is ToolResult.AccountTotalsResult -> {
                val period = (run.request as ToolRequest.ExpensesByAccount).period.label
                val focus = run.focusAccount
                if (focus != null) {
                    val item = result.items.firstOrNull { it.id == focus.id }
                        ?: com.kratt.finanzas.domain.model.LabeledTotal(focus.id, focus.name, 0L, 0)
                    AssistantAnswer.ExpensesByAccountAnswer(
                        period = period,
                        items = listOf(item),
                        action = AssistantAction.ViewMovements(accountId = focus.id),
                    )
                } else {
                    val sorted = result.items.sortedByDescending { it.totalCents }
                    AssistantAnswer.ExpensesByAccountAnswer(
                        period = period,
                        items = sorted,
                        action = sorted.firstOrNull()?.let { AssistantAction.ViewMovements(accountId = it.id) },
                    )
                }
            }

            is ToolResult.ComparisonResult -> {
                val request = run.request as ToolRequest.ComparePeriods
                AssistantAnswer.PeriodComparisonAnswer(
                    currentLabel = request.current.label,
                    previousLabel = request.previous.label,
                    income = result.income,
                    expense = result.expense,
                    net = result.net,
                )
            }

            is ToolResult.AccountBalancesResult -> {
                if (result.items.isEmpty()) {
                    AssistantAnswer.NoDataAnswer(NoDataKind.NO_ACCOUNTS)
                } else {
                    val total = safeSum(result.items.map { it.balance.currentBalanceCents })
                        ?: return AssistantAnswer.AmountTooLargeAnswer
                    AssistantAnswer.AccountBalancesAnswer(
                        items = result.items,
                        totalCents = total,
                        action = AssistantAction.OpenFeature(AssistantFeature.ACCOUNTS),
                    )
                }
            }

            is ToolResult.CreditCardDebtResult ->
                if (result.items.isEmpty()) {
                    AssistantAnswer.NoDataAnswer(NoDataKind.NO_CREDIT_CARDS)
                } else {
                    AssistantAnswer.CreditCardDebtAnswer(result.items)
                }

            is ToolResult.UpcomingPaymentsResult -> {
                if (result.commitments.isEmpty()) {
                    AssistantAnswer.NoDataAnswer(NoDataKind.NO_UPCOMING)
                } else {
                    val total = safeSum(result.commitments.map { it.amountCents })
                        ?: return AssistantAnswer.AmountTooLargeAnswer
                    val label = (run.request as ToolRequest.UpcomingPayments).label
                    AssistantAnswer.UpcomingPaymentsAnswer(
                        period = label,
                        count = result.commitments.size,
                        totalCents = total,
                        overdueCount = result.commitments.count { it.dueDate.isBefore(today) },
                    )
                }
            }

            is ToolResult.InstallmentProgressResult ->
                if (result.plans.isEmpty()) {
                    AssistantAnswer.NoDataAnswer(NoDataKind.NO_INSTALLMENTS)
                } else {
                    AssistantAnswer.InstallmentStatusAnswer(
                        plans = result.plans,
                        action = AssistantAction.OpenFeature(AssistantFeature.INSTALLMENTS),
                    )
                }

            is ToolResult.RecurringCommitmentsResult ->
                if (result.commitments.isEmpty()) {
                    AssistantAnswer.NoDataAnswer(NoDataKind.NO_UPCOMING)
                } else {
                    val total = safeSum(result.commitments.map { it.amountCents })
                        ?: return AssistantAnswer.AmountTooLargeAnswer
                    AssistantAnswer.UpcomingPaymentsAnswer(
                        period = PeriodLabel.Month(today.year, today.monthValue),
                        count = result.commitments.size,
                        totalCents = total,
                        overdueCount = result.commitments.count { it.dueDate.isBefore(today) },
                    )
                }

            is ToolResult.BudgetStatusResult -> {
                if (result.overall == null && result.categories.isEmpty()) {
                    AssistantAnswer.NoDataAnswer(NoDataKind.NO_BUDGETS)
                } else {
                    val month = (run.request as ToolRequest.BudgetStatus).month
                    AssistantAnswer.BudgetStatusAnswer(
                        period = PeriodLabel.Month(month.year, month.monthValue),
                        overall = result.overall,
                        categories = result.categories,
                        action = AssistantAction.OpenFeature(AssistantFeature.BUDGETS),
                    )
                }
            }

            is ToolResult.AvailableAfterCommitmentsResult -> {
                val month = (run.request as ToolRequest.AvailableAfterCommitments).month
                AssistantAnswer.AvailableAfterCommitmentsAnswer(
                    period = PeriodLabel.Month(month.year, month.monthValue),
                    incomeCents = result.incomeCents,
                    expenseCents = result.expenseCents,
                    committedCents = result.committedCents,
                    availableCents = result.availableCents,
                )
            }

            is ToolResult.SavingsGoalProgressResult ->
                if (result.goals.isEmpty()) {
                    AssistantAnswer.NoDataAnswer(NoDataKind.NO_GOALS)
                } else {
                    AssistantAnswer.SavingsGoalStatusAnswer(
                        goals = result.goals,
                        action = AssistantAction.OpenFeature(AssistantFeature.SAVINGS_GOALS),
                    )
                }

            is ToolResult.PlannedPurchaseReadinessResult ->
                if (result.purchases.isEmpty()) {
                    AssistantAnswer.NoDataAnswer(NoDataKind.NO_PURCHASES)
                } else {
                    AssistantAnswer.PlannedPurchaseStatusAnswer(
                        purchases = result.purchases,
                        action = AssistantAction.OpenFeature(AssistantFeature.PLANNED_PURCHASES),
                    )
                }

            is ToolResult.SearchResult -> {
                val request = run.request as ToolRequest.SearchTransactions
                val total = safeSum(result.items.map { it.amountCents })
                    ?: return AssistantAnswer.AmountTooLargeAnswer
                AssistantAnswer.TransactionSearchAnswer(
                    count = result.items.size,
                    totalCents = total,
                    sample = result.items.take(SEARCH_SAMPLE_SIZE),
                    filter = SearchFilterLabel(
                        queryText = run.searchTerm?.takeIf { it.isNotBlank() },
                        accountName = run.focusAccount?.name,
                        categoryName = run.focusCategory?.name,
                        period = request.period.label,
                    ),
                    action = AssistantAction.ViewMovements(
                        accountId = run.focusAccount?.id,
                        categoryId = run.focusCategory?.id,
                    ),
                )
            }

            is ToolResult.DuplicatesResult ->
                if (result.groups.isEmpty()) {
                    AssistantAnswer.NoDataAnswer(NoDataKind.NO_DUPLICATES)
                } else {
                    AssistantAnswer.PossibleDuplicatesAnswer(result.groups)
                }

            // el estado de respaldo no se enlaza a un plan de respuesta en esta fase
            is ToolResult.BackupStatusResult -> AssistantAnswer.ErrorAnswer
        }
    }

    private const val SEARCH_SAMPLE_SIZE = 5

    // suma segura; devuelve null si algun total desborda el rango soportado
    private fun safeSum(values: List<Long>): Long? = try {
        MoneyMath.sum(values)
    } catch (error: ArithmeticException) {
        null
    }
}
