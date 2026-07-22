package com.kratt.finanzas.domain.model

data class Category(
    val id: Long,
    val name: String,
    val transactionType: TransactionType,
    val iconKey: String,
    val isDefault: Boolean,
    val isActive: Boolean,
    val colorKey: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)
