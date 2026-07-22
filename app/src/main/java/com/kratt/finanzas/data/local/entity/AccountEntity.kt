package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kratt.finanzas.domain.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
    val initialBalanceCents: Long,
    val creditLimitCents: Long? = null,
    val lastFourDigits: String? = null,
    val description: String? = null,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
