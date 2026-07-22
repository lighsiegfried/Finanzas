package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.AccountTotals
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType

// agrupa los movimientos por cuenta para calcular saldos, todo en centavos
object AccountTotalsCalculator {

    fun totalsByAccount(transactions: List<Transaction>): Map<Long, AccountTotals> {
        // por cuenta: [ingresos, gastos, transferencias entrantes, transferencias salientes]
        val buckets = HashMap<Long, LongArray>()
        fun bucket(id: Long) = buckets.getOrPut(id) { LongArray(4) }
        for (t in transactions) {
            when (t.type) {
                TransactionType.INCOME -> bucket(t.accountId)[0] += t.amountCents
                TransactionType.EXPENSE -> bucket(t.accountId)[1] += t.amountCents
                TransactionType.TRANSFER -> {
                    // sale de la cuenta origen y entra a la cuenta destino
                    bucket(t.accountId)[3] += t.amountCents
                    t.destinationAccountId?.let { bucket(it)[0 + 2] += t.amountCents }
                }
            }
        }
        return buckets.mapValues { (_, v) -> AccountTotals(v[0], v[1], v[2], v[3]) }
    }

    fun totalsFor(accountId: Long, transactions: List<Transaction>): AccountTotals =
        totalsByAccount(transactions)[accountId] ?: AccountTotals.EMPTY
}
