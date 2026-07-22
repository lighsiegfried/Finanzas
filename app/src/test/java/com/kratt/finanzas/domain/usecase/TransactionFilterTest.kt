package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.TransactionListItem
import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionFilterTest {

    private val expense = TransactionListItem(
        id = 1, type = TransactionType.EXPENSE, amountCents = 12_575, description = "Café en la esquina",
        categoryName = "Alimentación", accountName = "Efectivo", destinationAccountName = null,
        accountId = 1, categoryId = 10, destinationAccountId = null, date = LocalDate.of(2026, 7, 5),
    )
    private val income = TransactionListItem(
        id = 2, type = TransactionType.INCOME, amountCents = 500_000, description = "Salario",
        categoryName = "Salario", accountName = "BAM", destinationAccountName = null,
        accountId = 2, categoryId = 20, destinationAccountId = null, date = LocalDate.of(2026, 7, 1),
    )
    private val transfer = TransactionListItem(
        id = 3, type = TransactionType.TRANSFER, amountCents = 100_000, description = null,
        categoryName = null, accountName = "Efectivo", destinationAccountName = "BAM",
        accountId = 1, categoryId = null, destinationAccountId = 2, date = LocalDate.of(2026, 7, 3),
    )
    private val all = listOf(expense, income, transfer)

    @Test
    fun emptyFilterReturnsAll() {
        assertEquals(all, TransactionSearch.apply(all, TransactionFilter()))
    }

    @Test
    fun filterByType() {
        val result = TransactionSearch.apply(all, TransactionFilter(type = TransactionType.TRANSFER))
        assertEquals(listOf(transfer), result)
    }

    @Test
    fun filterByAccountMatchesSourceOrDestination() {
        val result = TransactionSearch.apply(all, TransactionFilter(accountId = 2))
        // el ingreso usa la cuenta 2 y la transferencia la tiene como destino
        assertEquals(listOf(income, transfer), result)
    }

    @Test
    fun filterByCategory() {
        val result = TransactionSearch.apply(all, TransactionFilter(categoryId = 10))
        assertEquals(listOf(expense), result)
    }

    @Test
    fun searchIsAccentInsensitive() {
        val result = TransactionSearch.apply(all, TransactionFilter(query = "cafe"))
        assertEquals(listOf(expense), result)
    }

    @Test
    fun searchMatchesAccountAndCategoryNames() {
        assertEquals(listOf(income), TransactionSearch.apply(all, TransactionFilter(query = "bam", type = TransactionType.INCOME)))
        assertEquals(listOf(expense), TransactionSearch.apply(all, TransactionFilter(query = "aliment")))
    }

    @Test
    fun combinedFiltersNarrowResults() {
        val result = TransactionSearch.apply(
            all,
            TransactionFilter(type = TransactionType.TRANSFER, accountId = 1, query = "bam"),
        )
        assertEquals(listOf(transfer), result)
    }
}
