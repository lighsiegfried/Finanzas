package com.kratt.finanzas.domain.usecase

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportPeriodTest {

    private val today = LocalDate.of(2026, 7, 20)

    @Test
    fun deterministicRanges() {
        assertEquals(DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)), ReportPeriods.range(ReportPeriod.THIS_MONTH, today))
        assertEquals(DateRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)), ReportPeriods.range(ReportPeriod.LAST_MONTH, today))
        assertEquals(DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 7, 31)), ReportPeriods.range(ReportPeriod.LAST_3_MONTHS, today))
        assertEquals(DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 7, 31)), ReportPeriods.range(ReportPeriod.LAST_6_MONTHS, today))
        assertEquals(DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)), ReportPeriods.range(ReportPeriod.THIS_YEAR, today))
    }

    @Test
    fun customRangeUsesProvidedDates() {
        val r = ReportPeriods.range(ReportPeriod.CUSTOM, today, LocalDate.of(2026, 3, 5), LocalDate.of(2026, 4, 10))
        assertEquals(LocalDate.of(2026, 3, 5), r.start)
        assertEquals(LocalDate.of(2026, 4, 10), r.end)
    }

    @Test
    fun validityChecksStartBeforeEnd() {
        assertTrue(DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)).isValid)
        assertFalse(DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)).isValid)
    }
}
