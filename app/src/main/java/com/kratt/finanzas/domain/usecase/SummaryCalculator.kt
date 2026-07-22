package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.MonthlySummary
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType

object SummaryCalculator {

    // suma los ingresos y gastos de la lista y calcula el balance del mes
    fun calculate(transactions: List<Transaction>): MonthlySummary {
        var incomeCents = 0L
        var expenseCents = 0L
        for (transaction in transactions) {
            when (transaction.type) {
                TransactionType.INCOME -> incomeCents += transaction.amountCents
                TransactionType.EXPENSE -> expenseCents += transaction.amountCents
                // las transferencias no afectan ingresos ni gastos del mes
                TransactionType.TRANSFER -> Unit
            }
        }
        return MonthlySummary(
            incomeCents = incomeCents,
            expenseCents = expenseCents,
            balanceCents = incomeCents - expenseCents,
        )
    }
}
