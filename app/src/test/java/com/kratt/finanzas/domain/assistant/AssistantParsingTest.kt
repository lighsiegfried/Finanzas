package com.kratt.finanzas.domain.assistant

import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantParsingTest {

    private val today = LocalDate.of(2026, 7, 15)

    @Test
    fun classifies_expenses_by_category_specific() {
        val parsed = QueryParser.parse("¿Cuánto gasté en alimentación este mes?")
        assertEquals(AssistantIntent.EXPENSES_BY_CATEGORY, parsed.intent)
    }

    @Test
    fun classifies_top_category_question() {
        val parsed = QueryParser.parse("¿En qué categoría gasté más?")
        assertEquals(AssistantIntent.EXPENSES_BY_CATEGORY, parsed.intent)
    }

    @Test
    fun classifies_available_after_commitments() {
        val parsed = QueryParser.parse("¿Cuánto dinero me queda después de mis pagos pendientes?")
        assertEquals(AssistantIntent.AVAILABLE_AFTER_COMMITMENTS, parsed.intent)
    }

    @Test
    fun classifies_upcoming_payments_before_month_end() {
        val parsed = QueryParser.parse("¿Qué cuotas tengo antes de fin de mes?")
        assertEquals(AssistantIntent.UPCOMING_PAYMENTS, parsed.intent)
    }

    @Test
    fun classifies_installment_finish_question() {
        val parsed = QueryParser.parse("¿Cuándo termino de pagar mis cuotas?")
        assertEquals(AssistantIntent.INSTALLMENT_STATUS, parsed.intent)
    }

    @Test
    fun classifies_period_comparison() {
        val parsed = QueryParser.parse("¿Cómo cambió mi gasto durante los últimos tres meses?")
        assertEquals(AssistantIntent.PERIOD_COMPARISON, parsed.intent)
    }

    @Test
    fun classifies_savings_goal() {
        val parsed = QueryParser.parse("¿Cuánto falta para completar mi meta?")
        assertEquals(AssistantIntent.SAVINGS_GOAL_STATUS, parsed.intent)
    }

    @Test
    fun classifies_planned_purchase() {
        val parsed = QueryParser.parse("¿Puedo registrar la compra planificada sin superar mi presupuesto?")
        assertEquals(AssistantIntent.PLANNED_PURCHASE_STATUS, parsed.intent)
    }

    @Test
    fun classifies_possible_duplicates() {
        val parsed = QueryParser.parse("¿Qué movimientos parecen repetidos?")
        assertEquals(AssistantIntent.POSSIBLE_DUPLICATES, parsed.intent)
    }

    @Test
    fun classifies_budget_status() {
        val parsed = QueryParser.parse("¿Cómo va mi presupuesto?")
        assertEquals(AssistantIntent.BUDGET_STATUS, parsed.intent)
    }

    @Test
    fun classifies_help() {
        val parsed = QueryParser.parse("¿Qué puedes hacer?")
        assertEquals(AssistantIntent.HELP, parsed.intent)
    }

    @Test
    fun classifies_unsupported() {
        val parsed = QueryParser.parse("¿Cuál es el clima de hoy?")
        assertEquals(AssistantIntent.UNSUPPORTED, parsed.intent)
    }

    @Test
    fun parses_expense_draft_with_amount() {
        val parsed = QueryParser.parse("Registra Q200 de gasolina")
        val draft = parsed.draft
        assertNotNull(draft)
        assertEquals(TransactionType.EXPENSE, draft!!.type)
        assertEquals(20000L, draft.amountCents)
    }

    @Test
    fun parses_income_draft() {
        val parsed = QueryParser.parse("Agrega un ingreso de Q1500 de sueldo")
        assertEquals(TransactionType.INCOME, parsed.draft?.type)
        assertEquals(150000L, parsed.draft?.amountCents)
    }

    @Test
    fun write_verb_without_amount_is_not_a_draft() {
        val parsed = QueryParser.parse("Registra mis gastos por favor")
        assertNull(parsed.draft)
    }

    @Test
    fun resolves_this_month_by_default_without_period() {
        val resolution = PeriodResolver.resolve("cuánto gasté", today)
        val resolved = (resolution as PeriodResolution.Resolved).period
        assertEquals(PeriodLabel.Month(2026, 7), resolved.label)
        assertTrue(!resolved.wasExplicit)
    }

    @Test
    fun resolves_explicit_this_month() {
        val resolution = PeriodResolver.resolve("cuánto gasté este mes", today)
        val resolved = (resolution as PeriodResolution.Resolved).period
        assertEquals(PeriodLabel.Month(2026, 7), resolved.label)
        assertTrue(resolved.wasExplicit)
    }

    @Test
    fun resolves_last_month() {
        val resolution = PeriodResolver.resolve("mis gastos del mes pasado", today)
        val resolved = (resolution as PeriodResolution.Resolved).period
        assertEquals(PeriodLabel.Month(2026, 6), resolved.label)
    }

    @Test
    fun resolves_named_month_with_year() {
        val resolution = PeriodResolver.resolve("cuánto gasté en marzo de 2025", today)
        val resolved = (resolution as PeriodResolution.Resolved).period
        assertEquals(PeriodLabel.Month(2025, 3), resolved.label)
    }

    @Test
    fun resolves_named_month_without_year_uses_current_year() {
        val resolution = PeriodResolver.resolve("gastos de febrero", today)
        val resolved = (resolution as PeriodResolution.Resolved).period
        assertEquals(PeriodLabel.Month(2026, 2), resolved.label)
    }

    @Test
    fun resolves_last_three_months_range() {
        val resolution = PeriodResolver.resolve("los últimos tres meses", today)
        val resolved = (resolution as PeriodResolution.Resolved).period
        assertEquals(PeriodLabel.LastMonths(3), resolved.label)
        assertEquals(YearMonth.of(2026, 5).atDay(1), resolved.range.start)
        assertEquals(YearMonth.of(2026, 7).atEndOfMonth(), resolved.range.end)
    }

    @Test
    fun ambiguous_month_asks_for_clarification() {
        val resolution = PeriodResolver.resolve("cuánto gasté en el mes de", today)
        assertEquals(PeriodResolution.AmbiguousMonth, resolution)
    }

    @Test
    fun entity_resolves_unique_category_ignoring_accents() {
        val candidates = listOf(EntityCandidate(1, "Alimentación"), EntityCandidate(2, "Transporte"))
        val resolution = EntityResolver.resolve("cuánto gasté en alimentacion", candidates)
        assertEquals(EntityResolution.Unique(1, "Alimentación"), resolution)
    }

    @Test
    fun entity_returns_not_requested_when_no_match() {
        val candidates = listOf(EntityCandidate(1, "Alimentación"))
        val resolution = EntityResolver.resolve("cuánto gasté este mes", candidates)
        assertEquals(EntityResolution.NotRequested, resolution)
    }

    @Test
    fun entity_returns_ambiguous_when_two_unrelated_names_match() {
        val candidates = listOf(EntityCandidate(1, "Comida"), EntityCandidate(2, "Super"))
        val resolution = EntityResolver.resolve("gasté en comida y super", candidates)
        assertTrue(resolution is EntityResolution.Ambiguous)
        assertEquals(2, (resolution as EntityResolution.Ambiguous).options.size)
    }

    @Test
    fun entity_prefers_more_specific_nested_name() {
        val candidates = listOf(EntityCandidate(1, "Casa"), EntityCandidate(2, "Casa grande"))
        val resolution = EntityResolver.resolve("gasté en casa grande", candidates)
        assertEquals(EntityResolution.Unique(2, "Casa grande"), resolution)
    }
}
