package com.kratt.finanzas.domain.model

// plantilla reutilizable de movimiento; el monto puede quedar vacio
data class TransactionTemplate(
    val id: Long = 0L,
    val name: String,
    val type: TransactionType,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val categoryId: Long? = null,
    val defaultAmountCents: Long? = null,
    val description: String? = null,
    val isFavorite: Boolean = false,
    val isActive: Boolean = true,
    val lastUsedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
