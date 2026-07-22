package com.kratt.finanzas.domain.usecase

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class CommittedTotalsTest {

    @Test
    fun sumsOnlyCommitmentsOfTheSelectedMonth() {
        val commitments = listOf(
            Commitment(LocalDate.of(2026, 7, 5), 12_800),   // installment
            Commitment(LocalDate.of(2026, 7, 20), 20_000),  // recurring
            Commitment(LocalDate.of(2026, 8, 1), 5_000),    // otro mes
        )
        assertEquals(32_800L, CommittedTotals.forMonth(commitments, YearMonth.of(2026, 7)))
        assertEquals(5_000L, CommittedTotals.forMonth(commitments, YearMonth.of(2026, 8)))
    }

    @Test
    fun totalSumsAllCommitments() {
        val commitments = listOf(Commitment(LocalDate.of(2026, 7, 5), 100), Commitment(LocalDate.of(2026, 7, 6), 250))
        assertEquals(350L, CommittedTotals.total(commitments))
    }
}
