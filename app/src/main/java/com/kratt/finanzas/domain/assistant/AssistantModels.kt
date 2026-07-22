package com.kratt.finanzas.domain.assistant

import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.DateRange
import java.time.LocalDate

// intencion soportada por el asistente local, todo se resuelve en el dispositivo
enum class AssistantIntent {
    MONTHLY_SUMMARY,
    EXPENSES_BY_CATEGORY,
    EXPENSES_BY_ACCOUNT,
    ACCOUNT_BALANCES,
    CREDIT_CARD_DEBT,
    PERIOD_COMPARISON,
    AVAILABLE_AFTER_COMMITMENTS,
    UPCOMING_PAYMENTS,
    INSTALLMENT_STATUS,
    BUDGET_STATUS,
    SAVINGS_GOAL_STATUS,
    PLANNED_PURCHASE_STATUS,
    TRANSACTION_SEARCH,
    POSSIBLE_DUPLICATES,
    HELP,
    UNSUPPORTED,
}

// etiqueta del periodo lista para mostrar; el texto final se arma con recursos
sealed interface PeriodLabel {
    data class Month(val year: Int, val month: Int) : PeriodLabel
    data class LastMonths(val count: Int) : PeriodLabel
    data class Year(val year: Int) : PeriodLabel
    data class Custom(val start: LocalDate, val end: LocalDate) : PeriodLabel
}

// periodo ya resuelto: el rango real y su etiqueta; wasExplicit indica si el usuario lo pidio
data class ResolvedPeriod(
    val range: DateRange,
    val label: PeriodLabel,
    val wasExplicit: Boolean,
)

// resultado de interpretar el periodo pedido
sealed interface PeriodResolution {
    data class Resolved(val period: ResolvedPeriod) : PeriodResolution
    // el usuario menciono un mes pero no se pudo identificar cual
    object AmbiguousMonth : PeriodResolution
}

// un candidato para resolver una cuenta o categoria por nombre
data class EntityCandidate(val id: Long, val name: String)

// resultado de resolver una entidad mencionada por nombre
sealed interface EntityResolution {
    // no habia mencion de la entidad en la pregunta
    object NotRequested : EntityResolution
    data class Unique(val id: Long, val name: String) : EntityResolution
    // varias coincidencias, hay que preguntar cual
    data class Ambiguous(val options: List<EntityCandidate>) : EntityResolution
    // se menciono un nombre pero no coincide con ninguna entidad activa
    data class NotFound(val queryText: String) : EntityResolution
}

// nota de calidad de datos que se agrega cuando hace falta
enum class DataQuality {
    NONE,
    NO_DATA,
    PARTIAL_RANGE,
}

// tipo de aclaracion que el asistente necesita del usuario
enum class ClarificationKind {
    MISSING_PERIOD,
    AMBIGUOUS_ACCOUNT,
    AMBIGUOUS_CATEGORY,
    AMBIGUOUS_GOAL,
    AMBIGUOUS_PURCHASE,
    ACCOUNT_NOT_FOUND,
    CATEGORY_NOT_FOUND,
    GOAL_NOT_FOUND,
    MISSING_SEARCH_TERM,
}

// pantalla existente a la que el asistente puede llevar; la ui la traduce a una ruta
enum class AssistantFeature {
    ACCOUNTS,
    INSTALLMENTS,
    BUDGETS,
    SAVINGS_GOALS,
    PLANNED_PURCHASES,
}

// accion opcional que acompana una respuesta; el asistente nunca guarda datos por si mismo
sealed interface AssistantAction {
    // abre la lista de movimientos ya filtrada
    data class ViewMovements(
        val accountId: Long? = null,
        val categoryId: Long? = null,
        val type: TransactionType? = null,
    ) : AssistantAction

    // abre una pantalla existente de la app
    data class OpenFeature(val feature: AssistantFeature) : AssistantAction

    // prepara un borrador para revisar en el formulario normal, requiere guardar explicito
    data class ReviewDraft(val draft: DraftAction) : AssistantAction
}

// borrador de un movimiento propuesto; solo abre el formulario, nunca persiste
data class DraftAction(
    val type: TransactionType,
    val amountCents: Long,
    val categoryName: String? = null,
    val description: String? = null,
)
