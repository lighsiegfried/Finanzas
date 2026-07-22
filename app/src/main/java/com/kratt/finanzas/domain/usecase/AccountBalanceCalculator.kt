package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.AccountBalance
import com.kratt.finanzas.domain.model.AccountTotals
import com.kratt.finanzas.domain.model.AccountType

// calcula el saldo real de una cuenta con enteros, nunca con decimales
object AccountBalanceCalculator {

    fun calculate(account: Account, totals: AccountTotals): AccountBalance {
        return if (account.type == AccountType.CREDIT_CARD) {
            creditBalance(account, totals)
        } else {
            normalBalance(account, totals)
        }
    }

    // saldo normal: inicial mas ingresos menos gastos, ajustado por transferencias
    private fun normalBalance(account: Account, totals: AccountTotals): AccountBalance {
        // suma segura, detecta desbordes en vez de girar en silencio
        val balance = MoneyMath.sum(
            listOf(
                account.initialBalanceCents, totals.incomeCents, -totals.expenseCents,
                totals.transferInCents, -totals.transferOutCents,
            ),
        )
        return AccountBalance(
            accountId = account.id,
            currentBalanceCents = balance,
            isCredit = false,
            debtCents = 0L,
            hasCreditLimit = false,
            creditLimitCents = 0L,
            availableCreditCents = null,
        )
    }

    // deuda de tarjeta: gastos y cargos salientes menos pagos y reembolsos
    private fun creditBalance(account: Account, totals: AccountTotals): AccountBalance {
        // suma segura de la deuda, detecta desbordes
        val debt = MoneyMath.sum(
            listOf(
                account.initialBalanceCents, totals.expenseCents, totals.transferOutCents,
                -totals.incomeCents, -totals.transferInCents,
            ),
        )
        val limit = account.creditLimitCents
        // sin limite no se inventa un disponible falso
        val available = limit?.let { MoneyMath.subtract(it, debt) }
        return AccountBalance(
            accountId = account.id,
            currentBalanceCents = -debt,
            isCredit = true,
            debtCents = debt,
            hasCreditLimit = limit != null,
            creditLimitCents = limit ?: 0L,
            availableCreditCents = available,
        )
    }
}
