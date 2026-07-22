package com.kratt.finanzas.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyComparisonTest {

    @Test
    fun increaseComputesPercentage() {
        val c = MonthlyComparison.compare(previousCents = 1_000, currentCents = 1_500)
        assertEquals(500L, c.deltaCents)
        assertEquals(ChangeDirection.UP, c.direction)
        assertTrue(c.hasPrevious)
        assertEquals(50, c.percentAbs)
    }

    @Test
    fun decreaseAndSame() {
        assertEquals(ChangeDirection.DOWN, MonthlyComparison.compare(2_000, 1_000).direction)
        val same = MonthlyComparison.compare(1_000, 1_000)
        assertEquals(ChangeDirection.SAME, same.direction)
        assertEquals(0, same.percentAbs)
    }

    @Test
    fun previousZeroHasNoPercentage() {
        val c = MonthlyComparison.compare(0, 500)
        assertFalse(c.hasPrevious)
        assertNull(c.percentAbs)
        assertEquals(ChangeDirection.UP, c.direction)
    }

    @Test
    fun handlesNegativeValues() {
        // el balance neto puede ser negativo
        val c = MonthlyComparison.compare(-100, -300)
        assertEquals(-200L, c.deltaCents)
        assertEquals(ChangeDirection.DOWN, c.direction)
        assertEquals(200, c.percentAbs)
    }
}
