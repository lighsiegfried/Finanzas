package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.AccountTotals
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

// comprueba las invariantes financieras clave sobre conjuntos de movimientos deterministas
class Phase4aIntegrityTest {

    private fun tx(type: TransactionType, cents: Long, categoryId: Long? = null) = Transaction(
        id = 0L, accountId = 1L, type = type, amountCents = cents, description = null,
        date = LocalDate.of(2026, 7, 1), createdAtMillis = 0L, updatedAtMillis = 0L, categoryId = categoryId,
    )

    @Test
    fun net_equalsIncomeMinusExpenses() {
        val txs = listOf(tx(TransactionType.INCOME, 800_000), tx(TransactionType.EXPENSE, 525_000))
        val ie = ReportAggregator.incomeExpense(txs)
        assertEquals(800_000L, ie.incomeCents)
        assertEquals(525_000L, ie.expenseCents)
        assertEquals(275_000L, ie.netCents)
    }

    @Test
    fun transfers_doNotAffectIncomeOrExpense() {
        val txs = listOf(
            tx(TransactionType.INCOME, 100_000),
            tx(TransactionType.TRANSFER, 50_000),
            tx(TransactionType.EXPENSE, 20_000),
        )
        val ie = ReportAggregator.incomeExpense(txs)
        assertEquals(100_000L, ie.incomeCents)
        assertEquals(20_000L, ie.expenseCents)
    }

    @Test
    fun expensesByCategory_reconcilesWithTotalExpense() {
        val txs = listOf(
            tx(TransactionType.EXPENSE, 85_000, categoryId = 1L),
            tx(TransactionType.EXPENSE, 30_000, categoryId = 2L),
            tx(TransactionType.EXPENSE, 310_000, categoryId = 1L),
            tx(TransactionType.INCOME, 800_000, categoryId = 9L),
        )
        val byCategory = ReportAggregator.expensesByCategory(txs)
        val totalExpense = ReportAggregator.incomeExpense(txs).expenseCents
        // la suma de las categorias debe cuadrar con el gasto total reportado
        assertEquals(totalExpense, byCategory.values.sum())
        assertEquals(395_000L, byCategory[1L])
        assertEquals(30_000L, byCategory[2L])
    }

    @Test
    fun accountBalance_followsCanonicalFormula() {
        val account = Account(1L, "Efectivo", AccountType.CASH, "GTQ", 100_000, true)
        val totals = AccountTotals(incomeCents = 800_000, expenseCents = 525_000, transferInCents = 0, transferOutCents = 50_000)
        val balance = AccountBalanceCalculator.calculate(account, totals)
        // inicial 100000 + 800000 - 525000 + 0 - 50000 = 325000
        assertEquals(325_000L, balance.currentBalanceCents)
    }

    @Test
    fun budgetSpent_countsPostedExpensesOnly() {
        val txs = listOf(
            tx(TransactionType.EXPENSE, 85_000, categoryId = 1L),
            tx(TransactionType.INCOME, 800_000, categoryId = 1L),
            tx(TransactionType.TRANSFER, 50_000),
        )
        assertEquals(85_000L, BudgetSpentCalculator.spent(txs, categoryId = 1L))
        assertEquals(85_000L, BudgetSpentCalculator.spent(txs, categoryId = null))
    }
}
