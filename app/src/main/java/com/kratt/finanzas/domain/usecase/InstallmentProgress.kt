package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus

// vista minima de una cuota para calcular el avance del plan
data class OccurrenceView(val amountCents: Long, val status: InstallmentOccurrenceStatus)

data class InstallmentProgress(
    val totalCents: Long,
    val paidCents: Long,
    val remainingCents: Long,
    val paidCount: Int,
    val pendingCount: Int,
    val isComplete: Boolean,
)

object InstallmentProgressCalculator {

    // calcula pagado, pendiente y si el plan ya termino a partir de las cuotas
    fun calculate(occurrences: List<OccurrenceView>): InstallmentProgress {
        val total = occurrences.sumOf { it.amountCents }
        val paid = occurrences.filter { it.status == InstallmentOccurrenceStatus.PAID }.sumOf { it.amountCents }
        val paidCount = occurrences.count { it.status == InstallmentOccurrenceStatus.PAID }
        val pendingCount = occurrences.count {
            it.status == InstallmentOccurrenceStatus.PENDING || it.status == InstallmentOccurrenceStatus.OVERDUE
        }
        val remaining = occurrences
            .filter { it.status == InstallmentOccurrenceStatus.PENDING || it.status == InstallmentOccurrenceStatus.OVERDUE }
            .sumOf { it.amountCents }
        return InstallmentProgress(
            totalCents = total,
            paidCents = paid,
            remainingCents = remaining,
            paidCount = paidCount,
            pendingCount = pendingCount,
            isComplete = occurrences.isNotEmpty() && pendingCount == 0 && paidCount > 0,
        )
    }
}

// reglas para no pagar dos veces ni dejar una cuota pagada sin su movimiento
object InstallmentPaymentRules {
    fun canPay(status: InstallmentOccurrenceStatus, paidTransactionId: Long?): Boolean =
        (status == InstallmentOccurrenceStatus.PENDING || status == InstallmentOccurrenceStatus.OVERDUE) &&
            paidTransactionId == null

    fun canRevert(status: InstallmentOccurrenceStatus, paidTransactionId: Long?): Boolean =
        status == InstallmentOccurrenceStatus.PAID && paidTransactionId != null
}
