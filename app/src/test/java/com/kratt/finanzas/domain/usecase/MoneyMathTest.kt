package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.AccountTotals
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyMathTest {

    @Test
    fun maxSupportedAmount_isAccepted_andOverLimitRejected() {
        assertTrue(MoneyMath.isSupportedAmount(MoneyMath.MAX_SUPPORTED_CENTS))
        assertTrue(MoneyMath.isSupportedAmount(0L))
        assertFalse(MoneyMath.isSupportedAmount(MoneyMath.MAX_SUPPORTED_CENTS + 1))
        assertFalse(MoneyMath.isSupportedAmount(-1L))
    }

    @Test
    fun add_nearLongMax_throws() {
        assertThrows(ArithmeticException::class.java) { MoneyMath.add(Long.MAX_VALUE, 1L) }
    }

    @Test
    fun subtract_nearLongMin_throws() {
        assertThrows(ArithmeticException::class.java) { MoneyMath.subtract(Long.MIN_VALUE, 1L) }
    }

    @Test
    fun sum_withoutOverflow_matchesPlainSum() {
        assertEquals(6_000L, MoneyMath.sum(listOf(1_000L, 2_000L, 3_000L)))
    }

    @Test
    fun multiply_overflow_throws() {
        // la multiplicacion segura detecta el desborde de long
        assertThrows(ArithmeticException::class.java) { MoneyMath.multiply(Long.MAX_VALUE, 2L) }
    }

    @Test
    fun installmentDistribution_normalCase_sumsToTotal() {
        val amounts = InstallmentDistribution.distribute(153_600, 12)
        assertEquals(153_600L, amounts.sum())
        assertEquals(12, amounts.size)
    }

    @Test
    fun reportAggregation_incomeOverflow_throws() {
        val txs = listOf(
            tx(TransactionType.INCOME, Long.MAX_VALUE),
            tx(TransactionType.INCOME, 1L),
        )
        assertThrows(ArithmeticException::class.java) { ReportAggregator.incomeExpense(txs) }
    }

    @Test
    fun budgetAggregation_overflow_throws() {
        val txs = listOf(
            tx(TransactionType.EXPENSE, Long.MAX_VALUE, categoryId = 1L),
            tx(TransactionType.EXPENSE, 1L, categoryId = 1L),
        )
        assertThrows(ArithmeticException::class.java) { BudgetSpentCalculator.spent(txs, 1L) }
    }

    @Test
    fun accountBalance_overflow_throws() {
        val account = Account(1L, "Overflow", AccountType.CASH, "GTQ", Long.MAX_VALUE, true)
        val totals = AccountTotals(incomeCents = 1L)
        assertThrows(ArithmeticException::class.java) {
            AccountBalanceCalculator.calculate(account, totals)
        }
    }

    private fun tx(type: TransactionType, cents: Long, categoryId: Long? = null) = Transaction(
        id = 0L, accountId = 1L, type = type, amountCents = cents, description = null,
        date = LocalDate.of(2026, 7, 1), createdAtMillis = 0L, updatedAtMillis = 0L, categoryId = categoryId,
    )
}
