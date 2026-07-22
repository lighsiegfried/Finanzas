package com.kratt.finanzas.domain.model

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
    val initialBalanceCents: Long,
    val isActive: Boolean,
    val creditLimitCents: Long? = null,
    val lastFourDigits: String? = null,
    val description: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)
