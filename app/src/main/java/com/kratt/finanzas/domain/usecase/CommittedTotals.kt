package com.kratt.finanzas.domain.usecase

import java.time.LocalDate
import java.time.YearMonth

// un compromiso pendiente con su fecha y monto en centavos
data class Commitment(val dueDate: LocalDate, val amountCents: Long)

// suma los compromisos del mes sin mezclarlos con los gastos reales
object CommittedTotals {

    fun forMonth(commitments: List<Commitment>, month: YearMonth): Long =
        commitments.filter { YearMonth.from(it.dueDate) == month }.sumOf { it.amountCents }

    fun total(commitments: List<Commitment>): Long = commitments.sumOf { it.amountCents }
}
