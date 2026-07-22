package com.kratt.finanzas.domain.model

// totales del mes, el balance es ingresos menos gastos
data class MonthlySummary(
    val incomeCents: Long,
    val expenseCents: Long,
    val balanceCents: Long,
) {
    companion object {
        val EMPTY = MonthlySummary(0L, 0L, 0L)
    }
}
