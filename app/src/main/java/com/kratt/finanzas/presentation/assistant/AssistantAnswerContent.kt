package com.kratt.finanzas.presentation.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.assistant.AssistantAction
import com.kratt.finanzas.domain.assistant.AssistantAnswer
import com.kratt.finanzas.domain.assistant.AssistantFeature
import com.kratt.finanzas.domain.assistant.ClarificationKind
import com.kratt.finanzas.domain.assistant.NoDataKind
import com.kratt.finanzas.domain.assistant.PeriodLabel
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.BudgetState
import com.kratt.finanzas.domain.usecase.ChangeDirection
import com.kratt.finanzas.domain.usecase.MetricChange
import com.kratt.finanzas.domain.usecase.PurchaseReadiness
import com.kratt.finanzas.presentation.common.maskedAmount
import java.time.LocalDate
import kotlin.math.abs

// muestra una respuesta tipada del asistente en espanol; los montos se enmascaran segun la privacidad
@Composable
fun AssistantAnswerContent(
    answer: AssistantAnswer,
    onAction: (AssistantAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AnswerBody(answer)
        if (answer.isEstimate) {
            Text(
                text = stringResource(R.string.assistant_estimate),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        calculationSummary(answer)?.let { HowCalculated(it) }
        answer.action?.let { AssistantActionButton(it, onAction) }
    }
}

@Composable
private fun AnswerBody(answer: AssistantAnswer) {
    when (answer) {
        is AssistantAnswer.MonthlySummaryAnswer -> Line(
            stringResource(
                R.string.assistant_answer_summary,
                periodText(answer.period), money(answer.incomeCents), money(answer.expenseCents), money(answer.balanceCents),
            ),
        )

        is AssistantAnswer.ExpensesByCategoryAnswer -> {
            val items = answer.items
            when {
                items.isEmpty() -> Line(stringResource(R.string.assistant_answer_no_expenses, periodText(answer.period)))
                items.size == 1 -> {
                    val item = items.first()
                    if (item.movementCount == 0) {
                        Line(stringResource(R.string.assistant_answer_category_focus_none, periodText(answer.period), item.name))
                    } else {
                        Line(
                            stringResource(
                                R.string.assistant_answer_category_focus,
                                periodText(answer.period), money(item.totalCents), item.name, item.movementCount,
                            ),
                        )
                    }
                }
                else -> {
                    val top = items.first()
                    Line(stringResource(R.string.assistant_answer_category_top, periodText(answer.period), top.name, money(top.totalCents)))
                    items.drop(1).take(3).forEach { Detail(stringResource(R.string.assistant_line_name_value, it.name, money(it.totalCents))) }
                }
            }
        }

        is AssistantAnswer.ExpensesByAccountAnswer -> {
            val items = answer.items
            when {
                items.isEmpty() -> Line(stringResource(R.string.assistant_answer_no_expenses, periodText(answer.period)))
                items.size == 1 -> {
                    val item = items.first()
                    Line(
                        stringResource(
                            R.string.assistant_answer_account_focus,
                            periodText(answer.period), money(item.totalCents), item.name, item.movementCount,
                        ),
                    )
                }
                else -> {
                    val top = items.first()
                    Line(stringResource(R.string.assistant_answer_account_top, periodText(answer.period), top.name, money(top.totalCents)))
                    items.drop(1).take(3).forEach { Detail(stringResource(R.string.assistant_line_name_value, it.name, money(it.totalCents))) }
                }
            }
        }

        is AssistantAnswer.AccountBalancesAnswer -> {
            Line(stringResource(R.string.assistant_answer_balances_total, money(answer.totalCents)))
            answer.items.forEach { Detail(stringResource(R.string.assistant_line_name_value, it.name, money(it.balance.currentBalanceCents))) }
        }

        is AssistantAnswer.CreditCardDebtAnswer -> answer.items.forEach { item ->
            Line(stringResource(R.string.assistant_answer_debt_line, money(item.balance.debtCents), item.name))
            val available = item.balance.availableCreditCents
            if (item.balance.hasCreditLimit && available != null) {
                Detail(stringResource(R.string.assistant_answer_debt_available, money(available)))
            }
        }

        is AssistantAnswer.PeriodComparisonAnswer -> Line(
            stringResource(
                R.string.assistant_answer_comparison,
                periodText(answer.previousLabel), periodText(answer.currentLabel), changePhrase(answer.expense),
            ),
        )

        is AssistantAnswer.AvailableAfterCommitmentsAnswer -> Line(
            stringResource(
                R.string.assistant_answer_available,
                money(answer.expenseCents), money(answer.committedCents), money(answer.incomeCents), money(answer.availableCents),
            ),
        )

        is AssistantAnswer.UpcomingPaymentsAnswer -> {
            Line(stringResource(R.string.assistant_answer_upcoming, periodText(answer.period), answer.count, money(answer.totalCents)))
            if (answer.overdueCount > 0) Detail(stringResource(R.string.assistant_answer_upcoming_overdue, answer.overdueCount))
        }

        is AssistantAnswer.InstallmentStatusAnswer -> answer.plans.filterNot { it.progress.isComplete }.forEach { plan ->
            Line(
                stringResource(
                    R.string.assistant_answer_installment_line,
                    plan.planName, money(plan.progress.remainingCents), plan.progress.pendingCount,
                ),
            )
            plan.finishDate?.let { Detail(stringResource(R.string.assistant_answer_installment_finish, dateText(it))) }
        }

        is AssistantAnswer.BudgetStatusAnswer -> {
            answer.overall?.let {
                Line(
                    stringResource(
                        R.string.assistant_answer_budget_overall,
                        money(it.progress.spentCents), money(it.progress.limitCents), budgetStateText(it.progress.state),
                    ),
                )
            }
            answer.categories.forEach { line ->
                Detail(
                    stringResource(
                        R.string.assistant_answer_budget_line,
                        line.categoryName ?: "-", money(line.progress.spentCents), money(line.progress.limitCents), budgetStateText(line.progress.state),
                    ),
                )
            }
        }

        is AssistantAnswer.SavingsGoalStatusAnswer -> answer.goals.forEach { goal ->
            Line(
                stringResource(
                    R.string.assistant_answer_goal_line,
                    goal.goalName, money(goal.progress.contributedCents), money(goal.targetCents), goal.progress.progressPercent,
                ),
            )
            if (goal.progress.isComplete) {
                Detail(stringResource(R.string.assistant_answer_goal_complete))
            } else {
                Detail(stringResource(R.string.assistant_answer_goal_remaining, money(goal.progress.remainingCents)))
                goal.progress.suggestedMonthlyCents?.let { Detail(stringResource(R.string.assistant_answer_goal_suggested, money(it))) }
            }
        }

        is AssistantAnswer.PlannedPurchaseStatusAnswer -> answer.purchases.forEach { purchase ->
            Line(stringResource(R.string.assistant_answer_purchase_line, purchase.purchaseName, readinessText(purchase.readiness.readiness)))
            if (purchase.readiness.remainingToPurchaseCents > 0) {
                Detail(stringResource(R.string.assistant_answer_purchase_remaining, money(purchase.readiness.remainingToPurchaseCents)))
            }
        }

        is AssistantAnswer.TransactionSearchAnswer -> {
            if (answer.count == 0) {
                Line(stringResource(R.string.assistant_answer_search_none))
            } else {
                Line(stringResource(R.string.assistant_answer_search_found, answer.count, money(answer.totalCents)))
                answer.sample.forEach { Detail(stringResource(R.string.assistant_line_name_value, it.categoryName ?: it.accountName, money(it.amountCents))) }
            }
        }

        is AssistantAnswer.PossibleDuplicatesAnswer -> {
            Line(stringResource(R.string.assistant_answer_duplicates, answer.groups.size))
            answer.groups.take(5).forEach { Detail(stringResource(R.string.assistant_answer_duplicate_line, money(it.amountCents), it.accountName, it.count)) }
        }

        is AssistantAnswer.ClarificationAnswer -> {
            Line(clarificationText(answer.kind))
            answer.options.forEach { Detail("• ${it.name}") }
        }

        is AssistantAnswer.DraftAnswer -> {
            val type = if (answer.draft.type == TransactionType.INCOME) {
                stringResource(R.string.assistant_draft_type_income)
            } else {
                stringResource(R.string.assistant_draft_type_expense)
            }
            Line(stringResource(R.string.assistant_answer_draft, type, money(answer.draft.amountCents)))
        }

        is AssistantAnswer.NoDataAnswer -> Line(noDataText(answer.kind))
        AssistantAnswer.AmountTooLargeAnswer -> Line(stringResource(R.string.assistant_amount_too_large))
        AssistantAnswer.UnsupportedAnswer -> Line(stringResource(R.string.assistant_unsupported))
        AssistantAnswer.HelpAnswer -> Line(stringResource(R.string.assistant_help))
        AssistantAnswer.ErrorAnswer -> Line(stringResource(R.string.assistant_error))
    }
}

@Composable
private fun Line(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun Detail(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun HowCalculated(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag(TestTags.ASSISTANT_HOW_CALCULATED)) {
            Text(stringResource(R.string.assistant_how_calculated))
        }
        if (expanded) {
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AssistantActionButton(action: AssistantAction, onAction: (AssistantAction) -> Unit) {
    val label = when (action) {
        is AssistantAction.ViewMovements -> stringResource(R.string.assistant_view_movements)
        is AssistantAction.ReviewDraft -> stringResource(R.string.assistant_review_movement)
        is AssistantAction.OpenFeature -> when (action.feature) {
            AssistantFeature.ACCOUNTS -> stringResource(R.string.assistant_open_accounts)
            AssistantFeature.INSTALLMENTS -> stringResource(R.string.assistant_open_installments)
            AssistantFeature.BUDGETS -> stringResource(R.string.assistant_open_budgets)
            AssistantFeature.SAVINGS_GOALS -> stringResource(R.string.assistant_open_goals)
            AssistantFeature.PLANNED_PURCHASES -> stringResource(R.string.assistant_open_purchases)
        }
    }
    FilledTonalButton(onClick = { onAction(action) }, modifier = Modifier.testTag(TestTags.ASSISTANT_ACTION)) {
        Text(label)
    }
}

@Composable
private fun money(cents: Long): String = maskedAmount(cents)

@Composable
private fun periodText(label: PeriodLabel): String = when (label) {
    is PeriodLabel.Month -> stringResource(R.string.assistant_period_month, monthName(label.month), label.year)
    is PeriodLabel.LastMonths -> stringResource(R.string.assistant_period_last_months, label.count)
    is PeriodLabel.Year -> stringResource(R.string.assistant_period_year, label.year)
    is PeriodLabel.Custom -> "${dateText(label.start)} - ${dateText(label.end)}"
}

@Composable
private fun monthName(month: Int): String {
    val names = stringArrayResource(R.array.assistant_months)
    return names[(month - 1).coerceIn(0, 11)]
}

@Composable
private fun dateText(date: LocalDate): String =
    stringResource(R.string.assistant_period_month, monthName(date.monthValue), date.year).let { "${date.dayOfMonth} de $it" }

@Composable
private fun changePhrase(change: MetricChange): String {
    val amount = money(abs(change.deltaCents))
    return when (change.direction) {
        ChangeDirection.UP ->
            if (change.hasPrevious && change.percentAbs != null) {
                stringResource(R.string.assistant_change_up, amount, change.percentAbs)
            } else {
                stringResource(R.string.assistant_change_up_no_prev, amount)
            }
        ChangeDirection.DOWN ->
            if (change.hasPrevious && change.percentAbs != null) {
                stringResource(R.string.assistant_change_down, amount, change.percentAbs)
            } else {
                stringResource(R.string.assistant_change_down_no_prev, amount)
            }
        ChangeDirection.SAME -> stringResource(R.string.assistant_change_same)
    }
}

@Composable
private fun budgetStateText(state: BudgetState): String = when (state) {
    BudgetState.AVAILABLE -> stringResource(R.string.assistant_budget_state_available)
    BudgetState.WARNING -> stringResource(R.string.assistant_budget_state_warning)
    BudgetState.EXCEEDED -> stringResource(R.string.assistant_budget_state_exceeded)
}

@Composable
private fun readinessText(readiness: PurchaseReadiness): String = when (readiness) {
    PurchaseReadiness.NOT_FUNDED -> stringResource(R.string.assistant_readiness_not_funded)
    PurchaseReadiness.PARTIALLY_FUNDED -> stringResource(R.string.assistant_readiness_partial)
    PurchaseReadiness.READY -> stringResource(R.string.assistant_readiness_ready)
    PurchaseReadiness.PURCHASED -> stringResource(R.string.assistant_readiness_purchased)
}

@Composable
private fun clarificationText(kind: ClarificationKind): String = when (kind) {
    ClarificationKind.MISSING_PERIOD -> stringResource(R.string.assistant_clarify_period)
    ClarificationKind.AMBIGUOUS_ACCOUNT, ClarificationKind.ACCOUNT_NOT_FOUND -> stringResource(R.string.assistant_clarify_account)
    ClarificationKind.AMBIGUOUS_CATEGORY, ClarificationKind.CATEGORY_NOT_FOUND -> stringResource(R.string.assistant_clarify_category)
    ClarificationKind.AMBIGUOUS_GOAL, ClarificationKind.GOAL_NOT_FOUND -> stringResource(R.string.assistant_clarify_goal)
    ClarificationKind.AMBIGUOUS_PURCHASE -> stringResource(R.string.assistant_clarify_purchase)
    ClarificationKind.MISSING_SEARCH_TERM -> stringResource(R.string.assistant_clarify_search)
}

@Composable
private fun noDataText(kind: NoDataKind): String = when (kind) {
    NoDataKind.NO_ACCOUNTS -> stringResource(R.string.assistant_no_accounts)
    NoDataKind.NO_INSTALLMENTS -> stringResource(R.string.assistant_no_installments)
    NoDataKind.NO_BUDGETS -> stringResource(R.string.assistant_no_budgets)
    NoDataKind.NO_GOALS -> stringResource(R.string.assistant_no_goals)
    NoDataKind.NO_PURCHASES -> stringResource(R.string.assistant_no_purchases)
    NoDataKind.NO_UPCOMING -> stringResource(R.string.assistant_no_upcoming)
    NoDataKind.NO_CREDIT_CARDS -> stringResource(R.string.assistant_no_credit_cards)
    NoDataKind.NO_DUPLICATES -> stringResource(R.string.assistant_no_duplicates)
    NoDataKind.NO_SEARCH_RESULTS -> stringResource(R.string.assistant_no_search_results)
}

@Composable
private fun calculationSummary(answer: AssistantAnswer): String? = when (answer) {
    is AssistantAnswer.MonthlySummaryAnswer ->
        calcPeriod(answer.period) + " " + stringResource(R.string.assistant_calc_transfers_excluded)
    is AssistantAnswer.ExpensesByCategoryAnswer ->
        calcPeriod(answer.period) + " " + stringResource(R.string.assistant_calc_expense_posted)
    is AssistantAnswer.ExpensesByAccountAnswer ->
        calcPeriod(answer.period) + " " + stringResource(R.string.assistant_calc_expense_posted)
    is AssistantAnswer.PeriodComparisonAnswer ->
        calcPeriod(answer.currentLabel) + " " + stringResource(R.string.assistant_calc_transfers_excluded)
    is AssistantAnswer.AvailableAfterCommitmentsAnswer -> stringResource(R.string.assistant_calc_available)
    is AssistantAnswer.BudgetStatusAnswer -> stringResource(R.string.assistant_calc_budget)
    is AssistantAnswer.SavingsGoalStatusAnswer -> stringResource(R.string.assistant_calc_goal)
    else -> null
}

@Composable
private fun calcPeriod(label: PeriodLabel): String = stringResource(R.string.assistant_calc_period, periodText(label))
