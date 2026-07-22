package com.kratt.finanzas.domain.model

// presupuesto mensual en el dominio, categoryId nulo es el presupuesto general
data class Budget(
    val id: Long,
    val year: Int,
    val month: Int,
    val categoryId: Long? = null,
    val limitAmountCents: Long,
    val warningPercentage: Int,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
) {
    val isOverall: Boolean get() = categoryId == null
}
