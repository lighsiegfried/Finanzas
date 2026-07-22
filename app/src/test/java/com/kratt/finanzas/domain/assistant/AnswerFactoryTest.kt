package com.kratt.finanzas.domain.assistant

import com.kratt.finanzas.domain.model.AccountBalance
import com.kratt.finanzas.domain.model.LabeledTotal
import com.kratt.finanzas.domain.model.MonthlySummary
import com.kratt.finanzas.domain.usecase.DateRange
import com.kratt.finanzas.domain.usecase.MonthlyComparison
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerFactoryTest {

    private val today = LocalDate.of(2026, 7, 15)
    private val month = YearMonth.of(2026, 7)
    private val period = ResolvedPeriod(DateRange(month.atDay(1), month.atEndOfMonth()), PeriodLabel.Month(2026, 7), true)

    @Test
    fun maps_monthly_summary_result() {
        val run = AssistantPlan.Run(ToolRequest.MonthlySummary(month))
        val answer = AnswerFactory.build(run, ToolResult.MonthlySummaryResult(MonthlySummary(800_00, 525_00, 275_00)), today)
        val summary = answer as AssistantAnswer.MonthlySummaryAnswer
        assertEquals(PeriodLabel.Month(2026, 7), summary.period)
        assertEquals(275_00, summary.balanceCents)
        assertFalse(summary.isEstimate)
    }

    @Test
    fun focused_category_returns_single_item_and_view_action() {
        val run = AssistantPlan.Run(ToolRequest.ExpensesByCategory(period), focusCategory = EntityCandidate(1, "Alimentación"))
        val result = ToolResult.CategoryTotalsResult(
            listOf(LabeledTotal(1, "Alimentación", 850_00, 6), LabeledTotal(2, "Transporte", 300_00, 3)),
        )
        val answer = AnswerFactory.build(run, result, today) as AssistantAnswer.ExpensesByCategoryAnswer
        assertEquals(1, answer.items.size)
        assertEquals(850_00, answer.items.first().totalCents)
        assertEquals(1L, (answer.action as AssistantAction.ViewMovements).categoryId)
    }

    @Test
    fun category_breakdown_is_sorted_descending() {
        val run = AssistantPlan.Run(ToolRequest.ExpensesByCategory(period))
        val result = ToolResult.CategoryTotalsResult(
            listOf(LabeledTotal(2, "Transporte", 300_00, 3), LabeledTotal(1, "Alimentación", 850_00, 6)),
        )
        val answer = AnswerFactory.build(run, result, today) as AssistantAnswer.ExpensesByCategoryAnswer
        assertEquals(1L, answer.items.first().id)
    }

    @Test
    fun available_after_commitments_is_flagged_estimate() {
        val run = AssistantPlan.Run(ToolRequest.AvailableAfterCommitments(month))
        val result = ToolResult.AvailableAfterCommitmentsResult(800_00, 525_00, 80_00, 195_00)
        val answer = AnswerFactory.build(run, result, today)
        assertTrue(answer is AssistantAnswer.AvailableAfterCommitmentsAnswer)
        assertTrue(answer.isEstimate)
    }

    @Test
    fun overflow_in_totals_returns_amount_too_large() {
        val run = AssistantPlan.Run(ToolRequest.AccountBalances)
        val huge = AccountBalance(1, Long.MAX_VALUE, false, 0, false, 0, null)
        val result = ToolResult.AccountBalancesResult(
            listOf(NamedBalance("a", huge), NamedBalance("b", huge.copy(accountId = 2))),
        )
        assertEquals(AssistantAnswer.AmountTooLargeAnswer, AnswerFactory.build(run, result, today))
    }

    @Test
    fun empty_goals_returns_no_data() {
        val run = AssistantPlan.Run(ToolRequest.SavingsGoalProgress(null))
        val answer = AnswerFactory.build(run, ToolResult.SavingsGoalProgressResult(emptyList()), today)
        assertEquals(AssistantAnswer.NoDataAnswer(NoDataKind.NO_GOALS), answer)
    }

    @Test
    fun response_is_deterministic_for_same_input() {
        val run = AssistantPlan.Run(ToolRequest.MonthlySummary(month))
        val result = ToolResult.MonthlySummaryResult(MonthlySummary(800_00, 525_00, 275_00))
        assertEquals(AnswerFactory.build(run, result, today), AnswerFactory.build(run, result, today))
    }

    @Test
    fun maps_comparison_labels() {
        val previous = ResolvedPeriod(DateRange(YearMonth.of(2026, 6).atDay(1), YearMonth.of(2026, 6).atEndOfMonth()), PeriodLabel.Month(2026, 6), true)
        val run = AssistantPlan.Run(ToolRequest.ComparePeriods(period, previous))
        val result = ToolResult.ComparisonResult(
            income = MonthlyComparison.compare(100, 200),
            expense = MonthlyComparison.compare(300, 250),
            net = MonthlyComparison.compare(-200, -50),
        )
        val answer = AnswerFactory.build(run, result, today) as AssistantAnswer.PeriodComparisonAnswer
        assertEquals(PeriodLabel.Month(2026, 7), answer.currentLabel)
        assertEquals(PeriodLabel.Month(2026, 6), answer.previousLabel)
    }
}
