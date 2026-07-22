package com.kratt.finanzas.domain.assistant

import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.TextNormalizer

// resultado de interpretar la pregunta: la intencion y las pistas extraidas
data class ParsedQuery(
    val raw: String,
    val intent: AssistantIntent,
    // borrador propuesto cuando el usuario pide registrar algo; nunca se guarda solo
    val draft: DraftAction? = null,
    // frase objetivo para buscar o para resolver una categoria o cuenta
    val target: String? = null,
)

// clasifica la pregunta en espanol de forma determinista, sin enviar texto fuera del dispositivo
object QueryParser {

    private val writeVerbs = listOf(
        "registra", "registrar", "agrega", "agregar", "anota", "anotar", "apunta",
        "apuntar", "crea", "crear", "guarda", "guardar",
    )
    private val incomeWords = listOf(
        "ingreso", "ingresos", "me pagaron", "cobre", "recibi", "deposito", "sueldo", "salario",
    )
    private val amountRegex = Regex("q?\\s*(\\d{1,10}(?:[.,]\\d{1,2})?)")

    fun parse(rawQuery: String): ParsedQuery {
        val raw = rawQuery.trim()
        val text = TextNormalizer.normalize(raw)

        // primero un posible borrador de movimiento; requiere verbo de registro y un monto valido
        parseDraft(raw, text)?.let { draft ->
            return ParsedQuery(raw, AssistantIntent.UNSUPPORTED, draft = draft)
        }

        val intent = classify(text)
        val target = extractTarget(text)
        return ParsedQuery(raw, intent, target = target)
    }

    private fun classify(text: String): AssistantIntent = when {
        isHelp(text) -> AssistantIntent.HELP
        // rechaza solicitudes destructivas o de modificacion directa antes de clasificar el tema
        isDestructiveRequest(text) -> AssistantIntent.UNSUPPORTED
        hasAny(text, "repetid", "repit", "duplicad") -> AssistantIntent.POSSIBLE_DUPLICATES
        // disponible despues de compromisos
        (text.contains("me queda") || text.contains("disponible")) &&
            hasAny(text, "pago", "compromiso", "pendiente") -> AssistantIntent.AVAILABLE_AFTER_COMMITMENTS
        // compra planificada
        hasAny(text, "compra planificada", "compra planeada") ||
            (text.contains("puedo") && hasAny(text, "comprar", "compra")) -> AssistantIntent.PLANNED_PURCHASE_STATUS
        // cuando termino de pagar algo, aunque no diga la palabra cuota
        hasAny(text, "cuando termino de pagar", "cuando acabo de pagar", "cuando pago la ultima", "cuando saldo") ->
            AssistantIntent.INSTALLMENT_STATUS
        // cuando termino de pagar las cuotas
        text.contains("cuota") && hasAny(text, "cuando termino", "cuando acabo", "cuando pago", "cuando terminare", "falta para terminar", "cuando la termino") ->
            AssistantIntent.INSTALLMENT_STATUS
        // proximos pagos y cuotas por vencer
        hasAny(text, "proximo pago", "proximos pagos", "pagos proximos", "pagos pendientes", "por vencer", "antes de fin de mes", "proximas cuotas") ||
            (text.contains("cuota") && hasAny(text, "tengo", "pendiente", "este mes")) ->
            AssistantIntent.UPCOMING_PAYMENTS
        text.contains("cuota") -> AssistantIntent.INSTALLMENT_STATUS
        // deuda de tarjeta, antes que ahorro y saldos
        text.contains("tarjeta") && hasAny(text, "deuda", "debo", "saldo") -> AssistantIntent.CREDIT_CARD_DEBT
        hasAny(text, "cuanto debo", "mi deuda") -> AssistantIntent.CREDIT_CARD_DEBT
        // saldos de cuentas antes que metas, para que "saldo de ahorro" sea la cuenta Ahorro
        hasAny(text, "saldo", "cuanto tengo", "cuanto dinero tengo", "balance de mi cuenta") &&
            !text.contains("presupuesto") -> AssistantIntent.ACCOUNT_BALANCES
        // metas de ahorro, incluye ahorrado y ahorros
        hasAny(text, "meta", "ahorr") -> AssistantIntent.SAVINGS_GOAL_STATUS
        // comparacion de periodos, se revisa antes que el gasto simple
        hasAny(text, "compar", "como cambio", "como ha cambiado", "diferencia entre", " vs ", "versus") ->
            AssistantIntent.PERIOD_COMPARISON
        text.contains("presupuesto") -> AssistantIntent.BUDGET_STATUS
        // gasto por categoria
        mentionsSpending(text) && text.contains("categoria") -> AssistantIntent.EXPENSES_BY_CATEGORY
        // gasto por cuenta
        mentionsSpending(text) && text.contains("cuenta") -> AssistantIntent.EXPENSES_BY_ACCOUNT
        // gasto en algo especifico o total del mes
        isSpendQuestion(text) -> AssistantIntent.EXPENSES_BY_CATEGORY
        // resumen del mes
        hasAny(text, "resumen", "como voy", "como van mis finanzas", "cuanto gane", "cuanto ingrese", "mis ingresos", "balance del mes") ->
            AssistantIntent.MONTHLY_SUMMARY
        // busqueda de movimientos
        hasAny(text, "busca", "buscar", "movimientos de", "encuentra", "muestrame los movimientos") ->
            AssistantIntent.TRANSACTION_SEARCH
        else -> AssistantIntent.UNSUPPORTED
    }

    // verbos que piden modificar datos directamente; el asistente los rechaza (es de solo lectura)
    private val destructiveVerbs = listOf(
        "borra", "borrar", "elimina", "eliminar", "restaura", "restaurar", "desactiva", "desinstala",
        "paga", "modifica", "modificar", "cambia", "transfiere", "transferir",
    )

    private fun isDestructiveRequest(text: String): Boolean = destructiveVerbs.any { containsWord(text, it) }

    private fun mentionsSpending(text: String): Boolean =
        hasAny(text, "gaste", "gasto", "gastado", "gastos")

    private fun isSpendQuestion(text: String): Boolean =
        hasAny(text, "cuanto gaste", "cuanto he gastado", "cuanto llevo gastado", "mis gastos", "gaste en", "en que gaste")

    private fun isHelp(text: String): Boolean =
        hasAny(text, "que puedes hacer", "en que me puedes ayudar", "ayuda", "que preguntas", "como funciona", "que puedo preguntar")

    // detecta un borrador solo si hay verbo de registro y un monto valido
    private fun parseDraft(raw: String, text: String): DraftAction? {
        val hasWriteVerb = writeVerbs.any { containsWord(text, it) }
        if (!hasWriteVerb) return null
        val amountCents = extractAmountCents(text) ?: return null
        val type = if (incomeWords.any { text.contains(it) }) TransactionType.INCOME else TransactionType.EXPENSE
        val label = extractDraftLabel(raw)
        return DraftAction(type = type, amountCents = amountCents, categoryName = label, description = label)
    }

    private fun extractAmountCents(text: String): Long? {
        val match = amountRegex.find(text)?.groupValues?.get(1) ?: return null
        return AmountParser.parseToCents(match.replace(',', '.'))
    }

    // deja la parte descriptiva del borrador quitando verbos, monto y conectores
    private fun extractDraftLabel(raw: String): String? {
        val drop = writeVerbs + listOf(
            "un", "una", "de", "en", "por", "el", "la", "los", "las", "gasto", "gastos",
            "ingreso", "ingresos", "nuevo", "nueva",
        )
        val words = raw.split(Regex("\\s+")).filter { word ->
            val n = TextNormalizer.normalize(word)
            n.isNotBlank() &&
                n !in drop &&
                !amountRegex.matches(n) &&
                !n.matches(Regex("q?\\d.*"))
        }
        val label = words.joinToString(" ").trim()
        return label.ifBlank { null }
    }

    // frase objetivo para categoria, cuenta o busqueda: lo que sigue a "en" o "de"
    private fun extractTarget(text: String): String? {
        val match = Regex("(?:gaste en|gastado en|movimientos de|busca|buscar|de categoria|en la cuenta|en mi cuenta) (.+)")
            .find(text)?.groupValues?.get(1)?.trim()
        return match?.takeIf { it.isNotBlank() }
    }

    private fun hasAny(text: String, vararg needles: String): Boolean = needles.any { text.contains(it) }

    private fun containsWord(text: String, word: String): Boolean =
        Regex("(^|\\s)${Regex.escape(word)}(\\s|$)").containsMatchIn(text)
}
