package com.kratt.finanzas.domain.assistant

import com.kratt.finanzas.domain.model.TransactionListItem
import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateDetectorTest {

    private fun expense(id: Long, amount: Long, desc: String?, date: LocalDate, account: Long = 10) = TransactionListItem(
        id = id,
        type = TransactionType.EXPENSE,
        amountCents = amount,
        description = desc,
        categoryName = "Alimentación",
        accountName = "Efectivo",
        destinationAccountName = null,
        accountId = account,
        categoryId = 1,
        destinationAccountId = null,
        date = date,
    )

    @Test
    fun flags_two_similar_movements_within_window() {
        val items = listOf(
            expense(1, 5000, "Cafe", LocalDate.of(2026, 7, 1)),
            expense(2, 5000, "Cafe", LocalDate.of(2026, 7, 2)),
        )
        val groups = DuplicateDetector.detect(items, windowDays = 3)
        assertEquals(1, groups.size)
        assertEquals(2, groups.first().count)
    }

    @Test
    fun ignores_movements_far_apart() {
        val items = listOf(
            expense(1, 5000, "Cafe", LocalDate.of(2026, 7, 1)),
            expense(2, 5000, "Cafe", LocalDate.of(2026, 7, 20)),
        )
        assertTrue(DuplicateDetector.detect(items, windowDays = 3).isEmpty())
    }

    @Test
    fun ignores_different_amounts() {
        val items = listOf(
            expense(1, 5000, "Cafe", LocalDate.of(2026, 7, 1)),
            expense(2, 7000, "Cafe", LocalDate.of(2026, 7, 1)),
        )
        assertTrue(DuplicateDetector.detect(items, windowDays = 3).isEmpty())
    }

    @Test
    fun matches_description_ignoring_accents_and_case() {
        val items = listOf(
            expense(1, 5000, "Café", LocalDate.of(2026, 7, 1)),
            expense(2, 5000, "cafe", LocalDate.of(2026, 7, 2)),
        )
        assertEquals(1, DuplicateDetector.detect(items, windowDays = 3).size)
    }
}
