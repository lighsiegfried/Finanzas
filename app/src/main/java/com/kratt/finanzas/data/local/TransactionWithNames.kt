package com.kratt.finanzas.data.local

import com.kratt.finanzas.domain.model.TransactionType

// resultado del join de movimientos con los nombres de cuenta y categoria
// categoryName y destinationAccountName pueden ser nulos segun el tipo de movimiento
data class TransactionWithNames(
    val id: Long,
    val type: TransactionType,
    val amountCents: Long,
    val description: String?,
    val transactionDate: Long,
    val accountId: Long,
    val categoryId: Long?,
    val destinationAccountId: Long?,
    val accountName: String,
    val categoryName: String?,
    val destinationAccountName: String?,
)
