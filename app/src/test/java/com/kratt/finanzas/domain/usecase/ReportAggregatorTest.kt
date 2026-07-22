package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportAggregatorTest {

    private fun tx(type: TransactionType, amount: Long, categoryId: Long? = 1L) = Transaction(
        id = 0, accountId = 1, type = type, amountCents = amount, description = null,
        date = LocalDate.of(2026, 7, 1), createdAtMillis = 0, updatedAtMillis = 0,
        categoryId = if (type == TransactionType.TRANSFER) null else categoryId,
        destinationAccountId = if (type == TransactionType.TRANSFER) 2L else null,
    )

    private val sample = listOf(
        tx(TransactionType.INCOME, 800_000),
        tx(TransactionType.EXPENSE, 85_000, categoryId = 1),
        tx(TransactionType.EXPENSE, 30_000, categoryId = 2),
        tx(TransactionType.EXPENSE, 15_000, categoryId = 1),
        tx(TransactionType.TRANSFER, 100_000),
    )

    @Test
    fun incomeExpenseExcludesTransfers() {
        val ie = ReportAggregator.incomeExpense(sample)
        assertEquals(800_000L, ie.incomeCents)
        assertEquals(130_000L, ie.expenseCents)
        assertEquals(670_000L, ie.netCents)
    }

    @Test
    fun expensesGroupByCategory() {
        val byCat = ReportAggregator.expensesByCategory(sample)
        assertEquals(100_000L, byCat[1])
        assertEquals(30_000L, byCat[2])
        // el ingreso y la transferencia no aparecen
        assertEquals(2, byCat.size)
    }

    @Test
    fun percentageOfTotalIsSafe() {
        assertEquals(25, ReportAggregator.percentageOfTotal(25, 100))
        assertEquals(0, ReportAggregator.percentageOfTotal(25, 0))
    }

    @Test
    fun budgetSpentOnlyCountsExpensesOfScope() {
        // presupuesto general: todos los gastos, sin ingresos ni transferencias
        assertEquals(130_000L, BudgetSpentCalculator.spent(sample, null))
        // presupuesto de la categoria 1
        assertEquals(100_000L, BudgetSpentCalculator.spent(sample, 1))
    }
}
