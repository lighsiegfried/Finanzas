package com.kratt.finanzas.domain.model

// totales de ingresos y gastos de un periodo
data class IncomeExpense(val incomeCents: Long, val expenseCents: Long) {
    val netCents: Long get() = incomeCents - expenseCents
}

// total etiquetado con nombre, sirve para desgloses por categoria o cuenta
data class LabeledTotal(
    val id: Long,
    val name: String,
    val totalCents: Long,
    val movementCount: Int,
)

// fila de reporte por categoria
data class CategoryReportRow(
    val categoryId: Long,
    val name: String,
    val totalCents: Long,
    val movementCount: Int,
)

// fila de reporte por cuenta con saldo de apertura y cierre
data class AccountReportRow(
    val accountId: Long,
    val name: String,
    val openingCents: Long,
    val incomeCents: Long,
    val expenseCents: Long,
    val transferInCents: Long,
    val transferOutCents: Long,
    val closingCents: Long,
)

// un punto de la tendencia mensual
data class TrendPoint(
    val year: Int,
    val month: Int,
    val incomeCents: Long,
    val expenseCents: Long,
    val balanceCents: Long,
)
