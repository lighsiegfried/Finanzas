package com.kratt.finanzas.domain.usecase

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthRangeTest {

    @Test
    fun monthRange_coversFullJuly() {
        val range = MonthRange.of(YearMonth.of(2026, 7))
        assertEquals(LocalDate.of(2026, 7, 1).toEpochDay(), range.startEpochDay)
        assertEquals(LocalDate.of(2026, 7, 31).toEpochDay(), range.endEpochDay)
    }

    @Test
    fun monthRange_handlesLeapFebruary() {
        val range = MonthRange.of(YearMonth.of(2028, 2))
        assertEquals(LocalDate.of(2028, 2, 1).toEpochDay(), range.startEpochDay)
        assertEquals(LocalDate.of(2028, 2, 29).toEpochDay(), range.endEpochDay)
    }
}
