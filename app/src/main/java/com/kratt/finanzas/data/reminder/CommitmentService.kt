package com.kratt.finanzas.data.reminder

import com.kratt.finanzas.data.repository.InstallmentRepository
import com.kratt.finanzas.data.repository.RecurringRepository
import com.kratt.finanzas.domain.usecase.Commitment
import java.time.LocalDate

// orquesta la generacion de ocurrencias, el auto registro y la lista de compromisos
class CommitmentService(
    private val installmentRepository: InstallmentRepository,
    private val recurringRepository: RecurringRepository,
    private val today: () -> LocalDate = LocalDate::now,
) {

    // recalcula lo que quedo pendiente al abrir la app o al correr el worker
    suspend fun sync() {
        val date = today()
        installmentRepository.refreshOverdue(date)
        recurringRepository.generateDueOccurrences(date)
        recurringRepository.autoPostDue(date)
    }

    // compromisos de cuotas y recurrentes que vencen en el rango, sin duplicar
    suspend fun dueCommitments(start: LocalDate, end: LocalDate): List<Commitment> {
        val installments = installmentRepository.occurrencesDueBetween(start, end)
            .map { Commitment(it.dueDate, it.amountCents) }
        val recurring = recurringRepository.occurrencesDueBetween(start, end)
            .map { Commitment(it.scheduledDate, it.amountCents) }
        return installments + recurring
    }
}
