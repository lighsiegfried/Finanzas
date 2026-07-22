package com.kratt.finanzas.domain.model

import java.time.LocalDate

// version de un movimiento lista para mostrar, con nombres ya resueltos
// en una transferencia categoryName es nulo y destinationAccountName trae la cuenta destino
data class TransactionListItem(
    val id: Long,
    val type: TransactionType,
    val amountCents: Long,
    val description: String?,
    val categoryName: String?,
    val accountName: String,
    val destinationAccountName: String?,
    val accountId: Long,
    val categoryId: Long?,
    val destinationAccountId: Long?,
    val date: LocalDate,
)
