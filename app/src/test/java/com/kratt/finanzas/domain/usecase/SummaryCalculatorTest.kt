package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.MonthlySummary
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryCalculatorTest {

    // crea un movimiento minimo para las pruebas
    private fun transaction(type: TransactionType, cents: Long): Transaction = Transaction(
        id = 0L,
        accountId = 1L,
        categoryId = 1L,
        type = type,
        amountCents = cents,
        description = null,
        date = LocalDate.of(2026, 7, 10),
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    @Test
    fun incomeTotal_sumsOnlyIncomeTransactions() {
        val summary = SummaryCalculator.calculate(
            listOf(
                transaction(TransactionType.INCOME, 10_000),
                transaction(TransactionType.INCOME, 2_500),
                transaction(TransactionType.EXPENSE, 4_000),
            ),
        )
        assertEquals(12_500L, summary.incomeCents)
    }

    @Test
    fun expenseTotal_sumsOnlyExpenseTransactions() {
        val summary = SummaryCalculator.calculate(
            listOf(
                transaction(TransactionType.INCOME, 10_000),
                transaction(TransactionType.EXPENSE, 4_000),
                transaction(TransactionType.EXPENSE, 1_250),
            ),
        )
        assertEquals(5_250L, summary.expenseCents)
    }

    @Test
    fun monthlyBalance_isIncomeMinusExpenses() {
        val summary = SummaryCalculator.calculate(
            listOf(
                transaction(TransactionType.INCOME, 10_000),
                transaction(TransactionType.EXPENSE, 4_000),
            ),
        )
        assertEquals(6_000L, summary.balanceCents)
    }

    @Test
    fun monthlyBalance_canBeNegative() {
        val summary = SummaryCalculator.calculate(
            listOf(
                transaction(TransactionType.INCOME, 1_000),
                transaction(TransactionType.EXPENSE, 12_575),
            ),
        )
        assertEquals(-11_575L, summary.balanceCents)
    }

    @Test
    fun emptyList_producesAllZeros() {
        assertEquals(MonthlySummary.EMPTY, SummaryCalculator.calculate(emptyList()))
    }
}
