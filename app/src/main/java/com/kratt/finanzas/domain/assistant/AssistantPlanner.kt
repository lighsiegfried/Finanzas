package com.kratt.finanzas.domain.assistant

import java.time.LocalDate
import java.time.YearMonth

// candidatos disponibles para resolver entidades mencionadas por nombre
data class PlanContextData(
    val accounts: List<EntityCandidate> = emptyList(),
    val categories: List<EntityCandidate> = emptyList(),
    val goals: List<EntityCandidate> = emptyList(),
    val purchases: List<EntityCandidate> = emptyList(),
)

// decision del asistente: ejecutar una herramienta, pedir aclaracion, o una respuesta directa
sealed interface AssistantPlan {
    data class Run(
        val request: ToolRequest,
        val focusCategory: EntityCandidate? = null,
        val focusAccount: EntityCandidate? = null,
        val searchTerm: String? = null,
    ) : AssistantPlan

    data class Clarify(val kind: ClarificationKind, val options: List<EntityCandidate> = emptyList()) : AssistantPlan
    data class Draft(val draft: DraftAction) : AssistantPlan
    object Help : AssistantPlan
    object Unsupported : AssistantPlan
}

// planifica la respuesta de forma pura; recibe los candidatos y decide sin tocar la base
object AssistantPlanner {

    fun plan(parsed: ParsedQuery, context: PlanContextData, today: LocalDate): AssistantPlan {
        parsed.draft?.let { return AssistantPlan.Draft(it) }

        return when (parsed.intent) {
            AssistantIntent.HELP -> AssistantPlan.Help
            AssistantIntent.UNSUPPORTED -> AssistantPlan.Unsupported
            AssistantIntent.POSSIBLE_DUPLICATES ->
                AssistantPlan.Run(ToolRequest.DetectPossibleDuplicates(windowDays = 3))
            AssistantIntent.ACCOUNT_BALANCES -> AssistantPlan.Run(ToolRequest.AccountBalances)
            AssistantIntent.CREDIT_CARD_DEBT -> AssistantPlan.Run(ToolRequest.CreditCardDebt)
            AssistantIntent.INSTALLMENT_STATUS -> AssistantPlan.Run(ToolRequest.InstallmentProgress)

            AssistantIntent.MONTHLY_SUMMARY -> withPeriod(parsed, today) { period ->
                if (period.label is PeriodLabel.Month) {
                    AssistantPlan.Run(ToolRequest.MonthlySummary(monthOf(period)))
                } else {
                    AssistantPlan.Run(ToolRequest.IncomeExpenseForPeriod(period))
                }
            }

            AssistantIntent.AVAILABLE_AFTER_COMMITMENTS -> withPeriod(parsed, today) { period ->
                AssistantPlan.Run(ToolRequest.AvailableAfterCommitments(monthOf(period)))
            }

            AssistantIntent.EXPENSES_BY_CATEGORY -> withPeriod(parsed, today) { period ->
                when (val category = EntityResolver.resolve(parsed.raw, context.categories)) {
                    is EntityResolution.Ambiguous ->
                        AssistantPlan.Clarify(ClarificationKind.AMBIGUOUS_CATEGORY, category.options)
                    is EntityResolution.Unique ->
                        AssistantPlan.Run(ToolRequest.ExpensesByCategory(period), focusCategory = EntityCandidate(category.id, category.name))
                    else -> AssistantPlan.Run(ToolRequest.ExpensesByCategory(period))
                }
            }

            AssistantIntent.EXPENSES_BY_ACCOUNT -> withPeriod(parsed, today) { period ->
                when (val account = EntityResolver.resolve(parsed.raw, context.accounts)) {
                    is EntityResolution.Ambiguous ->
                        AssistantPlan.Clarify(ClarificationKind.AMBIGUOUS_ACCOUNT, account.options)
                    is EntityResolution.Unique ->
                        AssistantPlan.Run(ToolRequest.ExpensesByAccount(period), focusAccount = EntityCandidate(account.id, account.name))
                    else -> AssistantPlan.Run(ToolRequest.ExpensesByAccount(period))
                }
            }

            AssistantIntent.PERIOD_COMPARISON -> withPeriod(parsed, today) { period ->
                val current = if (period.label is PeriodLabel.Month) period else PeriodResolver.defaultMonth(today)
                val previous = PeriodResolver.previousMonthOf(current)
                AssistantPlan.Run(ToolRequest.ComparePeriods(current, previous))
            }

            AssistantIntent.UPCOMING_PAYMENTS -> withPeriod(parsed, today) { period ->
                val month = monthOf(period)
                AssistantPlan.Run(
                    ToolRequest.UpcomingPayments(start = today, end = month.atEndOfMonth(), label = period.label),
                )
            }

            AssistantIntent.BUDGET_STATUS -> withPeriod(parsed, today) { period ->
                AssistantPlan.Run(ToolRequest.BudgetStatus(monthOf(period)))
            }

            AssistantIntent.SAVINGS_GOAL_STATUS ->
                when (val goal = EntityResolver.resolve(parsed.raw, context.goals)) {
                    is EntityResolution.Ambiguous ->
                        AssistantPlan.Clarify(ClarificationKind.AMBIGUOUS_GOAL, goal.options)
                    is EntityResolution.Unique -> AssistantPlan.Run(ToolRequest.SavingsGoalProgress(goal.id))
                    else -> AssistantPlan.Run(ToolRequest.SavingsGoalProgress(null))
                }

            AssistantIntent.PLANNED_PURCHASE_STATUS ->
                when (val purchase = EntityResolver.resolve(parsed.raw, context.purchases)) {
                    is EntityResolution.Ambiguous ->
                        AssistantPlan.Clarify(ClarificationKind.AMBIGUOUS_PURCHASE, purchase.options)
                    is EntityResolution.Unique -> AssistantPlan.Run(ToolRequest.PlannedPurchaseReadiness(purchase.id))
                    else -> AssistantPlan.Run(ToolRequest.PlannedPurchaseReadiness(null))
                }

            AssistantIntent.TRANSACTION_SEARCH -> withPeriod(parsed, today) { period ->
                val category = EntityResolver.resolve(parsed.raw, context.categories)
                val account = EntityResolver.resolve(parsed.raw, context.accounts)
                val focusCategory = (category as? EntityResolution.Unique)?.let { EntityCandidate(it.id, it.name) }
                val focusAccount = (account as? EntityResolution.Unique)?.let { EntityCandidate(it.id, it.name) }
                val term = parsed.target
                if (term.isNullOrBlank() && focusCategory == null && focusAccount == null) {
                    AssistantPlan.Clarify(ClarificationKind.MISSING_SEARCH_TERM)
                } else {
                    AssistantPlan.Run(
                        ToolRequest.SearchTransactions(
                            queryText = term ?: "",
                            accountId = focusAccount?.id,
                            categoryId = focusCategory?.id,
                            period = period,
                        ),
                        focusCategory = focusCategory,
                        focusAccount = focusAccount,
                        searchTerm = term,
                    )
                }
            }
        }
    }

    // resuelve el periodo o pide aclaracion si el mes es ambiguo
    private inline fun withPeriod(parsed: ParsedQuery, today: LocalDate, block: (ResolvedPeriod) -> AssistantPlan): AssistantPlan =
        when (val resolution = PeriodResolver.resolve(parsed.raw, today)) {
            is PeriodResolution.Resolved -> block(resolution.period)
            PeriodResolution.AmbiguousMonth -> AssistantPlan.Clarify(ClarificationKind.MISSING_PERIOD)
        }

    private fun monthOf(period: ResolvedPeriod): YearMonth = when (val label = period.label) {
        is PeriodLabel.Month -> YearMonth.of(label.year, label.month)
        else -> YearMonth.from(period.range.start)
    }
}
