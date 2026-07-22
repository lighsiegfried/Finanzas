package com.kratt.finanzas.domain.model

// totales de movimientos de una cuenta, siempre en centavos
data class AccountTotals(
    val incomeCents: Long = 0L,
    val expenseCents: Long = 0L,
    val transferInCents: Long = 0L,
    val transferOutCents: Long = 0L,
) {
    companion object {
        val EMPTY = AccountTotals()
    }
}

// saldo calculado de una cuenta, con soporte para tarjetas de credito
data class AccountBalance(
    val accountId: Long,
    val currentBalanceCents: Long,
    val isCredit: Boolean,
    val debtCents: Long,
    val hasCreditLimit: Boolean,
    val creditLimitCents: Long,
    val availableCreditCents: Long?,
)
