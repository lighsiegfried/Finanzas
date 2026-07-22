package com.kratt.finanzas.domain.assistant

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPlannerTest {

    private val today = LocalDate.of(2026, 7, 15)
    private val categories = listOf(EntityCandidate(1, "Alimentación"), EntityCandidate(2, "Transporte"))
    private val accounts = listOf(EntityCandidate(10, "Efectivo"), EntityCandidate(11, "Banco"))
    private val goals = listOf(EntityCandidate(20, "Viaje"), EntityCandidate(21, "Emergencia"))

    private fun plan(query: String, context: PlanContextData = PlanContextData()): AssistantPlan =
        AssistantPlanner.plan(QueryParser.parse(query), context, today)

    @Test
    fun expenses_by_category_with_known_category_runs_focused_tool() {
        val result = plan("cuánto gasté en alimentación", PlanContextData(categories = categories))
        assertTrue(result is AssistantPlan.Run)
        val run = result as AssistantPlan.Run
        assertTrue(run.request is ToolRequest.ExpensesByCategory)
        assertEquals(1L, run.focusCategory?.id)
    }

    @Test
    fun ambiguous_category_asks_for_clarification() {
        val ambiguous = listOf(EntityCandidate(1, "Comida"), EntityCandidate(2, "Super"))
        val result = plan("gasté en comida y super", PlanContextData(categories = ambiguous))
        assertEquals(ClarificationKind.AMBIGUOUS_CATEGORY, (result as AssistantPlan.Clarify).kind)
    }

    @Test
    fun ambiguous_account_asks_for_clarification() {
        val ambiguous = listOf(EntityCandidate(1, "Banco uno"), EntityCandidate(2, "Banco dos"))
        val result = plan("cuánto gasté en la cuenta banco uno banco dos", PlanContextData(accounts = ambiguous))
        assertEquals(ClarificationKind.AMBIGUOUS_ACCOUNT, (result as AssistantPlan.Clarify).kind)
    }

    @Test
    fun ambiguous_month_asks_for_period() {
        val result = plan("cuánto gasté en el mes de")
        assertEquals(ClarificationKind.MISSING_PERIOD, (result as AssistantPlan.Clarify).kind)
    }

    @Test
    fun ambiguous_goal_asks_for_clarification() {
        val result = plan("cuánto falta para mi meta viaje emergencia", PlanContextData(goals = goals))
        assertEquals(ClarificationKind.AMBIGUOUS_GOAL, (result as AssistantPlan.Clarify).kind)
    }

    @Test
    fun monthly_summary_for_month_uses_monthly_tool() {
        val result = plan("cuánto gané este mes")
        assertTrue((result as AssistantPlan.Run).request is ToolRequest.MonthlySummary)
    }

    @Test
    fun monthly_summary_for_year_uses_period_tool() {
        val result = plan("cuánto gané este año")
        assertTrue((result as AssistantPlan.Run).request is ToolRequest.IncomeExpenseForPeriod)
    }

    @Test
    fun period_comparison_builds_current_and_previous() {
        val result = plan("cómo cambió mi gasto este mes") as AssistantPlan.Run
        val request = result.request as ToolRequest.ComparePeriods
        assertEquals(PeriodLabel.Month(2026, 7), request.current.label)
        assertEquals(PeriodLabel.Month(2026, 6), request.previous.label)
    }

    @Test
    fun write_request_returns_draft_not_execution() {
        val result = plan("Registra Q200 de gasolina")
        assertTrue(result is AssistantPlan.Draft)
        assertEquals(20000L, (result as AssistantPlan.Draft).draft.amountCents)
    }

    @Test
    fun search_without_term_asks_for_clarification() {
        val result = plan("buscar")
        assertEquals(ClarificationKind.MISSING_SEARCH_TERM, (result as AssistantPlan.Clarify).kind)
    }

    @Test
    fun help_and_unsupported_map_directly() {
        assertTrue(plan("¿qué puedes hacer?") is AssistantPlan.Help)
        assertTrue(plan("¿cuál es el clima?") is AssistantPlan.Unsupported)
    }
}
