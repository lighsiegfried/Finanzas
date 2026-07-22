package com.kratt.finanzas.domain.assistant

import com.kratt.finanzas.domain.model.LabeledTotal
import com.kratt.finanzas.domain.model.TransactionListItem
import com.kratt.finanzas.domain.usecase.MetricChange

// motivo por el que no hay datos suficientes para responder
enum class NoDataKind {
    NO_ACCOUNTS,
    NO_INSTALLMENTS,
    NO_BUDGETS,
    NO_GOALS,
    NO_PURCHASES,
    NO_UPCOMING,
    NO_CREDIT_CARDS,
    NO_DUPLICATES,
    NO_SEARCH_RESULTS,
}

// etiqueta de los filtros aplicados en una busqueda, para explicar la respuesta
data class SearchFilterLabel(
    val queryText: String?,
    val accountName: String?,
    val categoryName: String?,
    val period: PeriodLabel,
)

// respuesta tipada del asistente; el texto en espanol se arma en la capa de presentacion
// a partir de estos datos, asi los montos se enmascaran segun la privacidad al mostrarlos
sealed interface AssistantAnswer {
    val action: AssistantAction? get() = null
    // un dato calculado que no es un saldo exacto se marca como estimacion
    val isEstimate: Boolean get() = false

    data class MonthlySummaryAnswer(
        val period: PeriodLabel,
        val incomeCents: Long,
        val expenseCents: Long,
        val balanceCents: Long,
    ) : AssistantAnswer

    data class ExpensesByCategoryAnswer(
        val period: PeriodLabel,
        val items: List<LabeledTotal>,
        override val action: AssistantAction?,
    ) : AssistantAnswer

    data class ExpensesByAccountAnswer(
        val period: PeriodLabel,
        val items: List<LabeledTotal>,
        override val action: AssistantAction?,
    ) : AssistantAnswer

    data class AccountBalancesAnswer(
        val items: List<NamedBalance>,
        val totalCents: Long,
        override val action: AssistantAction?,
    ) : AssistantAnswer

    data class CreditCardDebtAnswer(
        val items: List<NamedBalance>,
    ) : AssistantAnswer

    data class PeriodComparisonAnswer(
        val currentLabel: PeriodLabel,
        val previousLabel: PeriodLabel,
        val income: MetricChange,
        val expense: MetricChange,
        val net: MetricChange,
    ) : AssistantAnswer

    data class AvailableAfterCommitmentsAnswer(
        val period: PeriodLabel,
        val incomeCents: Long,
        val expenseCents: Long,
        val committedCents: Long,
        val availableCents: Long,
    ) : AssistantAnswer {
        override val isEstimate: Boolean get() = true
    }

    data class UpcomingPaymentsAnswer(
        val period: PeriodLabel,
        val count: Int,
        val totalCents: Long,
        val overdueCount: Int,
    ) : AssistantAnswer

    data class InstallmentStatusAnswer(
        val plans: List<InstallmentPlanStatus>,
        override val action: AssistantAction?,
    ) : AssistantAnswer

    data class BudgetStatusAnswer(
        val period: PeriodLabel,
        val overall: BudgetLine?,
        val categories: List<BudgetLine>,
        override val action: AssistantAction?,
    ) : AssistantAnswer

    data class SavingsGoalStatusAnswer(
        val goals: List<GoalStatus>,
        override val action: AssistantAction?,
    ) : AssistantAnswer {
        // la fecha estimada de una meta es una estimacion, no una promesa
        override val isEstimate: Boolean get() = goals.any { it.progress.estimatedDate != null }
    }

    data class PlannedPurchaseStatusAnswer(
        val purchases: List<PurchaseStatusItem>,
        override val action: AssistantAction?,
    ) : AssistantAnswer

    data class TransactionSearchAnswer(
        val count: Int,
        val totalCents: Long,
        val sample: List<TransactionListItem>,
        val filter: SearchFilterLabel,
        override val action: AssistantAction?,
    ) : AssistantAnswer

    data class PossibleDuplicatesAnswer(
        val groups: List<DuplicateGroup>,
    ) : AssistantAnswer

    // el asistente necesita una aclaracion antes de responder
    data class ClarificationAnswer(
        val kind: ClarificationKind,
        val options: List<EntityCandidate> = emptyList(),
    ) : AssistantAnswer

    // borrador de un movimiento; solo abre el formulario normal para revisar y guardar
    data class DraftAnswer(
        val draft: DraftAction,
    ) : AssistantAnswer {
        override val action: AssistantAction get() = AssistantAction.ReviewDraft(draft)
    }

    data class NoDataAnswer(val kind: NoDataKind) : AssistantAnswer

    // el monto pedido o calculado es demasiado grande para representarlo con seguridad
    object AmountTooLargeAnswer : AssistantAnswer

    // pregunta no soportada; la ui ofrece sugerencias
    object UnsupportedAnswer : AssistantAnswer

    object HelpAnswer : AssistantAnswer

    // fallo interno inesperado; mensaje generico, nunca detalles tecnicos
    object ErrorAnswer : AssistantAnswer
}
