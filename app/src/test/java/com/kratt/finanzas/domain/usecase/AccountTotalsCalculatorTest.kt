package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountTotalsCalculatorTest {

    private fun tx(
        type: TransactionType,
        amount: Long,
        accountId: Long,
        destinationAccountId: Long? = null,
    ) = Transaction(
        id = 0L,
        accountId = accountId,
        type = type,
        amountCents = amount,
        description = null,
        date = LocalDate.of(2026, 7, 1),
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
        categoryId = if (type == TransactionType.TRANSFER) null else 1L,
        destinationAccountId = destinationAccountId,
    )

    @Test
    fun incomeAndExpenseAffectOnlyTheirAccount() {
        val totals = AccountTotalsCalculator.totalsFor(
            1L,
            listOf(tx(TransactionType.INCOME, 5000, 1L), tx(TransactionType.EXPENSE, 2000, 1L)),
        )
        assertEquals(5000L, totals.incomeCents)
        assertEquals(2000L, totals.expenseCents)
        assertEquals(0L, totals.transferInCents)
        assertEquals(0L, totals.transferOutCents)
    }

    @Test
    fun transferAffectsBothAccountsButNotIncomeOrExpense() {
        val list = listOf(tx(TransactionType.TRANSFER, 3000, accountId = 1L, destinationAccountId = 2L))
        val source = AccountTotalsCalculator.totalsFor(1L, list)
        val destination = AccountTotalsCalculator.totalsFor(2L, list)

        assertEquals(3000L, source.transferOutCents)
        assertEquals(0L, source.transferInCents)
        assertEquals(0L, source.incomeCents)
        assertEquals(0L, source.expenseCents)

        assertEquals(3000L, destination.transferInCents)
        assertEquals(0L, destination.transferOutCents)
        assertEquals(0L, destination.incomeCents)
        assertEquals(0L, destination.expenseCents)
    }
}
