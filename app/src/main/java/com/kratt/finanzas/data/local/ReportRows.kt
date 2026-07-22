package com.kratt.finanzas.data.local

// resultado agregado de gastos por categoria
data class CategoryTotalRow(
    val categoryId: Long,
    val name: String,
    val totalCents: Long,
    val movementCount: Int,
)

// resultado agregado de gastos por cuenta
data class AccountTotalRow(
    val accountId: Long,
    val name: String,
    val totalCents: Long,
    val movementCount: Int,
)
