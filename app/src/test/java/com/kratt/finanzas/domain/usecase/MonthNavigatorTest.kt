package com.kratt.finanzas.domain.usecase

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthNavigatorTest {

    @Test
    fun previousCrossesYearBoundary() {
        assertEquals(YearMonth.of(2025, 12), MonthNavigator.previous(YearMonth.of(2026, 1)))
    }

    @Test
    fun nextCrossesYearBoundary() {
        assertEquals(YearMonth.of(2027, 1), MonthNavigator.next(YearMonth.of(2026, 12)))
    }

    @Test
    fun isCurrentReset() {
        val today = YearMonth.of(2026, 7)
        assertTrue(MonthNavigator.isCurrent(today, today))
        assertFalse(MonthNavigator.isCurrent(YearMonth.of(2026, 6), today))
    }
}
