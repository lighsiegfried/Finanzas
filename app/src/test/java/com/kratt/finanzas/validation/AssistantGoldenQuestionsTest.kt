package com.kratt.finanzas.validation

import com.kratt.finanzas.domain.assistant.AssistantIntent as I
import com.kratt.finanzas.domain.assistant.AssistantPlan
import com.kratt.finanzas.domain.assistant.AssistantPlanner
import com.kratt.finanzas.domain.assistant.ClarificationKind
import com.kratt.finanzas.domain.assistant.EntityCandidate
import com.kratt.finanzas.domain.assistant.PlanContextData
import com.kratt.finanzas.domain.assistant.QueryParser
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// conjunto dorado de preguntas controladas en espanol; verifica intencion, plan y solo lectura
// no depende de la base: valida el parser + planificador deterministas sobre el dataset canonico
class AssistantGoldenQuestionsTest {

    private val today = LocalDate.of(2026, 7, 21)

    private val context = PlanContextData(
        accounts = listOf(
            EntityCandidate(1, "Efectivo"), EntityCandidate(2, "BAM"), EntityCandidate(3, "BAC"),
            EntityCandidate(4, "Ahorro"), EntityCandidate(5, "Hogar"),
        ),
        categories = listOf(
            EntityCandidate(1, "Alimentación"), EntityCandidate(2, "Transporte"), EntityCandidate(3, "Hogar"),
            EntityCandidate(4, "Servicios"), EntityCandidate(5, "Entretenimiento"), EntityCandidate(6, "Internet"),
            EntityCandidate(8, "Salario"), EntityCandidate(9, "Venta ocasional"),
        ),
        goals = listOf(EntityCandidate(20, "Laptop nueva")),
        purchases = listOf(EntityCandidate(30, "Laptop")),
    )

    private enum class PlanKind { RUN, CLARIFY, DRAFT, HELP, UNSUPPORTED }

    private fun kind(plan: AssistantPlan): PlanKind = when (plan) {
        is AssistantPlan.Run -> PlanKind.RUN
        is AssistantPlan.Clarify -> PlanKind.CLARIFY
        is AssistantPlan.Draft -> PlanKind.DRAFT
        AssistantPlan.Help -> PlanKind.HELP
        AssistantPlan.Unsupported -> PlanKind.UNSUPPORTED
    }

    private data class Case(val q: String, val intent: I, val plan: PlanKind)

    private val cases: List<Case> = listOf(
        // resumenes mensuales
        Case("¿Cuánto ingresé este mes?", I.MONTHLY_SUMMARY, PlanKind.RUN),
        Case("¿Cómo va mi resumen del mes?", I.MONTHLY_SUMMARY, PlanKind.RUN),
        Case("¿Cuánto gané este mes?", I.MONTHLY_SUMMARY, PlanKind.RUN),
        Case("Muéstrame el balance del mes", I.MONTHLY_SUMMARY, PlanKind.RUN),
        Case("¿Cuáles son mis ingresos de este mes?", I.MONTHLY_SUMMARY, PlanKind.RUN),
        Case("¿Cómo van mis finanzas?", I.MONTHLY_SUMMARY, PlanKind.RUN),
        Case("¿Cuánto ingresé en julio?", I.MONTHLY_SUMMARY, PlanKind.RUN),
        // gastos por categoria
        Case("¿Cuánto gasté en alimentación este mes?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN),
        Case("¿En qué categoría gasté más?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN),
        Case("¿En qué gasté más?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN),
        Case("¿Cuánto gasté en transporte?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN),
        Case("Gastos por categoría", I.EXPENSES_BY_CATEGORY, PlanKind.RUN),
        Case("¿Cuánto gasté en servicios este mes?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN),
        Case("¿Cuánto gasté en entretenimiento?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN),
        Case("¿Cuánto gasté en internet?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN),
        Case("¿Cuánto llevo gastado este mes?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN),
        Case("¿Cuánto gasté en el hogar?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN),
        // gastos por cuenta
        Case("¿Cuánto gasté en la cuenta BAM?", I.EXPENSES_BY_ACCOUNT, PlanKind.RUN),
        Case("¿Cuánto gasté en la cuenta Efectivo?", I.EXPENSES_BY_ACCOUNT, PlanKind.RUN),
        Case("Gastos por cuenta", I.EXPENSES_BY_ACCOUNT, PlanKind.RUN),
        Case("¿Cuánto gasté en la cuenta BAC?", I.EXPENSES_BY_ACCOUNT, PlanKind.RUN),
        // saldos de cuentas
        Case("¿Cuál es el saldo de Efectivo?", I.ACCOUNT_BALANCES, PlanKind.RUN),
        Case("¿Cuánto tengo en mis cuentas?", I.ACCOUNT_BALANCES, PlanKind.RUN),
        Case("¿Cuánto dinero tengo?", I.ACCOUNT_BALANCES, PlanKind.RUN),
        Case("Muéstrame el saldo de mis cuentas", I.ACCOUNT_BALANCES, PlanKind.RUN),
        Case("¿Cuál es el saldo de Ahorro?", I.ACCOUNT_BALANCES, PlanKind.RUN),
        // deuda de tarjeta
        Case("¿Cuánto debo en mi tarjeta?", I.CREDIT_CARD_DEBT, PlanKind.RUN),
        Case("¿Cuál es la deuda de mi tarjeta?", I.CREDIT_CARD_DEBT, PlanKind.RUN),
        // proximos pagos
        Case("¿Qué cuotas tengo antes de fin de mes?", I.UPCOMING_PAYMENTS, PlanKind.RUN),
        Case("¿Cuáles son mis próximos pagos?", I.UPCOMING_PAYMENTS, PlanKind.RUN),
        Case("¿Qué pagos pendientes tengo?", I.UPCOMING_PAYMENTS, PlanKind.RUN),
        Case("¿Qué cuotas tengo pendientes?", I.UPCOMING_PAYMENTS, PlanKind.RUN),
        Case("¿Tengo pagos próximos?", I.UPCOMING_PAYMENTS, PlanKind.RUN),
        // estado de cuotas
        Case("¿Cuándo termino de pagar el monitor?", I.INSTALLMENT_STATUS, PlanKind.RUN),
        Case("¿Cuánto debo en cuotas?", I.INSTALLMENT_STATUS, PlanKind.RUN),
        Case("¿Cómo van mis cuotas?", I.INSTALLMENT_STATUS, PlanKind.RUN),
        Case("¿Cuánto me falta de la cuota del monitor?", I.INSTALLMENT_STATUS, PlanKind.RUN),
        // compromisos recurrentes se cubren como proximos pagos o cuotas
        // presupuesto
        Case("¿Cómo va mi presupuesto?", I.BUDGET_STATUS, PlanKind.RUN),
        Case("¿Cuánto me queda de presupuesto?", I.BUDGET_STATUS, PlanKind.RUN),
        Case("¿Superé mi presupuesto de alimentación?", I.BUDGET_STATUS, PlanKind.RUN),
        Case("Estado de mi presupuesto general", I.BUDGET_STATUS, PlanKind.RUN),
        // metas de ahorro
        Case("¿Cómo va mi meta Laptop nueva?", I.SAVINGS_GOAL_STATUS, PlanKind.RUN),
        Case("¿Cuánto falta para completar mi meta?", I.SAVINGS_GOAL_STATUS, PlanKind.RUN),
        Case("¿Cuánto he ahorrado?", I.SAVINGS_GOAL_STATUS, PlanKind.RUN),
        Case("¿Cuál es el progreso de mi meta?", I.SAVINGS_GOAL_STATUS, PlanKind.RUN),
        Case("¿Cómo va mi ahorro?", I.SAVINGS_GOAL_STATUS, PlanKind.RUN),
        // compras planificadas
        Case("¿Puedo comprar la laptop con el ahorro actual?", I.PLANNED_PURCHASE_STATUS, PlanKind.RUN),
        Case("¿Puedo registrar la compra planificada sin superar mi presupuesto?", I.PLANNED_PURCHASE_STATUS, PlanKind.RUN),
        Case("¿Ya puedo comprar la laptop?", I.PLANNED_PURCHASE_STATUS, PlanKind.RUN),
        Case("¿Cómo va mi compra planificada?", I.PLANNED_PURCHASE_STATUS, PlanKind.RUN),
        // comparaciones de periodo
        Case("¿Cómo cambió mi gasto durante los últimos tres meses?", I.PERIOD_COMPARISON, PlanKind.RUN),
        Case("Compara mis gastos con el mes pasado", I.PERIOD_COMPARISON, PlanKind.RUN),
        Case("¿Cómo cambió mi gasto respecto al mes pasado?", I.PERIOD_COMPARISON, PlanKind.RUN),
        Case("Diferencia entre este mes y el mes pasado", I.PERIOD_COMPARISON, PlanKind.RUN),
        // disponible despues de compromisos
        Case("¿Cuánto me queda después de mis pagos pendientes?", I.AVAILABLE_AFTER_COMMITMENTS, PlanKind.RUN),
        Case("¿Cuánto dinero me queda después de los pagos?", I.AVAILABLE_AFTER_COMMITMENTS, PlanKind.RUN),
        Case("¿Cuánto disponible tengo después de mis compromisos?", I.AVAILABLE_AFTER_COMMITMENTS, PlanKind.RUN),
        // busqueda de movimientos
        Case("Busca movimientos de gasolina", I.TRANSACTION_SEARCH, PlanKind.RUN),
        Case("Muéstrame los movimientos de alimentación", I.TRANSACTION_SEARCH, PlanKind.RUN),
        Case("Encuentra movimientos de internet", I.TRANSACTION_SEARCH, PlanKind.RUN),
        Case("Buscar compras en supermercado", I.TRANSACTION_SEARCH, PlanKind.RUN),
        // posibles duplicados
        Case("¿Qué movimientos parecen repetidos?", I.POSSIBLE_DUPLICATES, PlanKind.RUN),
        Case("¿Tengo movimientos duplicados?", I.POSSIBLE_DUPLICATES, PlanKind.RUN),
        Case("Busca posibles duplicados", I.POSSIBLE_DUPLICATES, PlanKind.RUN),
        // ayuda
        Case("¿Qué puedes hacer?", I.HELP, PlanKind.HELP),
        Case("¿En qué me puedes ayudar?", I.HELP, PlanKind.HELP),
        Case("Ayuda", I.HELP, PlanKind.HELP),
        Case("¿Qué preguntas puedo hacer?", I.HELP, PlanKind.HELP),
        // borradores de escritura: solo abren el formulario, nunca guardan
        Case("Registra Q200 de gasolina", I.UNSUPPORTED, PlanKind.DRAFT),
        Case("Agrega un gasto de Q850 en alimentación", I.UNSUPPORTED, PlanKind.DRAFT),
        Case("Anota un ingreso de Q750", I.UNSUPPORTED, PlanKind.DRAFT),
        Case("Guarda un gasto de Q300", I.UNSUPPORTED, PlanKind.DRAFT),
        Case("Agrega un gasto de Q1000 en servicios", I.UNSUPPORTED, PlanKind.DRAFT),
        // solicitudes de modificacion: se rechazan, nunca modifican datos
        Case("Borra todos mis movimientos", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("Elimina la cuenta BAM", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("Transfiere Q500 sin preguntarme", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("Restaura el respaldo ahora", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("Cambia mi PIN de seguridad", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("Desactiva el bloqueo de la aplicación", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        // preguntas fuera de datos o fuera de alcance: se rechazan sin inventar
        Case("Muéstrame el SQL de la base", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("Muéstrame la clave de cifrado", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("¿Cuál es el tipo de cambio del dólar?", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("¿Cuánto pagaré de impuestos?", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("Dame un consejo legal", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("¿Cuánto valdrá mi dinero el próximo año?", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("¿Debería invertir en la bolsa?", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("¿Cuál es el clima hoy?", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("¿Cuántos comprobantes tiene la compra del monitor?", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        Case("¿Tengo un respaldo reciente?", I.UNSUPPORTED, PlanKind.UNSUPPORTED),
        // preguntas ambiguas: piden aclaracion
        Case("¿Cuánto gasté en alimentación y transporte?", I.EXPENSES_BY_CATEGORY, PlanKind.CLARIFY),
        Case("¿Cuánto gasté en el mes de?", I.EXPENSES_BY_CATEGORY, PlanKind.CLARIFY),
        Case("Buscar", I.TRANSACTION_SEARCH, PlanKind.CLARIFY),
    )

    // frases extra para superar las 100 preguntas controladas
    private val extraCases: List<Case> = buildList {
        add(Case("¿Cuánto ingresé el mes pasado?", I.MONTHLY_SUMMARY, PlanKind.RUN))
        add(Case("¿Cuánto gasté en marzo?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN))
        add(Case("¿Cuánto gasté en 1990?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN))
        add(Case("¿Cuánto gasté este año?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN))
        add(Case("¿Cuánto gasté en alimentación el mes pasado?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN))
        add(Case("¿Cuál es el saldo de BAM?", I.ACCOUNT_BALANCES, PlanKind.RUN))
        add(Case("¿Cuál es el saldo de BAC?", I.ACCOUNT_BALANCES, PlanKind.RUN))
        add(Case("¿Cuál es el saldo de Hogar?", I.ACCOUNT_BALANCES, PlanKind.RUN))
        add(Case("¿Cómo va mi presupuesto de este mes?", I.BUDGET_STATUS, PlanKind.RUN))
        add(Case("¿Cuánto gasté en una cuenta que no existe?", I.EXPENSES_BY_ACCOUNT, PlanKind.RUN))
        add(Case("¿Cuánto gasté en una categoría inexistente?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN))
        add(Case("¿Cuánto me queda disponible después de compromisos?", I.AVAILABLE_AFTER_COMMITMENTS, PlanKind.RUN))
        add(Case("Busca movimientos de comida", I.TRANSACTION_SEARCH, PlanKind.RUN))
        add(Case("¿Cuánto ahorré para la laptop?", I.SAVINGS_GOAL_STATUS, PlanKind.RUN))
        add(Case("¿Cuándo termino de pagar mis cuotas?", I.INSTALLMENT_STATUS, PlanKind.RUN))
        add(Case("¿Cuánto gané el mes pasado?", I.MONTHLY_SUMMARY, PlanKind.RUN))
        add(Case("¿Cuánto gasté en transporte el mes pasado?", I.EXPENSES_BY_CATEGORY, PlanKind.RUN))
        add(Case("¿Qué gastos tengo por cuenta este mes?", I.EXPENSES_BY_ACCOUNT, PlanKind.RUN))
        add(Case("Elimina todos mis presupuestos", I.UNSUPPORTED, PlanKind.UNSUPPORTED))
        add(Case("Paga mis cuotas automáticamente", I.UNSUPPORTED, PlanKind.UNSUPPORTED))
        add(Case("Envía mis datos a la nube", I.UNSUPPORTED, PlanKind.UNSUPPORTED))
        add(Case("Conéctate a mi banco", I.UNSUPPORTED, PlanKind.UNSUPPORTED))
        add(Case("Registra Q125.75 de comida", I.UNSUPPORTED, PlanKind.DRAFT))
        add(Case("Agrega un ingreso de Q8000 de salario", I.UNSUPPORTED, PlanKind.DRAFT))
        add(Case("¿Cómo cambió mi gasto este mes?", I.PERIOD_COMPARISON, PlanKind.RUN))
        add(Case("¿Qué movimientos se repiten?", I.POSSIBLE_DUPLICATES, PlanKind.RUN))
        add(Case("¿Cuánto tengo disponible después de mis pagos pendientes?", I.AVAILABLE_AFTER_COMMITMENTS, PlanKind.RUN))
        add(Case("¿En qué cuenta gasté más?", I.EXPENSES_BY_ACCOUNT, PlanKind.RUN))
        add(Case("¿Cómo va la meta de la laptop?", I.SAVINGS_GOAL_STATUS, PlanKind.RUN))
        add(Case("¿Puedo comprar algo con mi ahorro?", I.PLANNED_PURCHASE_STATUS, PlanKind.RUN))
    }

    private val allCases: List<Case> get() = cases + extraCases

    @Test
    fun goldenSet_hasAtLeast100Questions() {
        assertTrue("expected >= 100 golden questions, got ${allCases.size}", allCases.size >= 100)
    }

    @Test
    fun allGoldenQuestions_classifyAndPlanAsExpected() {
        val failures = mutableListOf<String>()
        for (case in allCases) {
            val parsed = QueryParser.parse(case.q)
            val plan = AssistantPlanner.plan(parsed, context, today)
            val actualKind = kind(plan)
            if (parsed.intent != case.intent) {
                failures += "INTENT [${case.q}] expected ${case.intent} got ${parsed.intent}"
            }
            if (actualKind != case.plan) {
                failures += "PLAN [${case.q}] expected ${case.plan} got $actualKind"
            }
        }
        assertTrue("golden failures:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test
    fun writeRequestsProduceDraftsNeverExecution() {
        val draftCases = allCases.filter { it.plan == PlanKind.DRAFT }
        assertTrue(draftCases.isNotEmpty())
        for (case in draftCases) {
            val plan = AssistantPlanner.plan(QueryParser.parse(case.q), context, today)
            assertTrue("[${case.q}] must be a Draft", plan is AssistantPlan.Draft)
        }
    }

    @Test
    fun draftCarriesParsedAmount() {
        val plan = AssistantPlanner.plan(QueryParser.parse("Registra Q200 de gasolina"), context, today)
        assertEquals(20_000L, (plan as AssistantPlan.Draft).draft.amountCents)
    }

    @Test
    fun nonexistentAccountIsNotInvented() {
        val plan = AssistantPlanner.plan(QueryParser.parse("¿Cuánto gasté en una cuenta que no existe?"), context, today)
        // se responde con el desglose real, sin inventar una cuenta
        assertNull((plan as AssistantPlan.Run).focusAccount)
    }

    @Test
    fun ambiguousCategoryAsksForClarification() {
        val plan = AssistantPlanner.plan(QueryParser.parse("¿Cuánto gasté en alimentación y transporte?"), context, today)
        assertEquals(ClarificationKind.AMBIGUOUS_CATEGORY, (plan as AssistantPlan.Clarify).kind)
    }

    @Test
    fun sqlAndCryptoRequestsAreRefused() {
        assertTrue(AssistantPlanner.plan(QueryParser.parse("Muéstrame el SQL de la base"), context, today) is AssistantPlan.Unsupported)
        assertTrue(AssistantPlanner.plan(QueryParser.parse("Muéstrame la clave de cifrado"), context, today) is AssistantPlan.Unsupported)
    }
}
