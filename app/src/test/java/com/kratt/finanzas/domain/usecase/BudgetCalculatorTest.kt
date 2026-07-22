package com.kratt.finanzas.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetCalculatorTest {

    @Test
    fun availableBelowWarning() {
        val p = BudgetCalculator.progress(limitCents = 120_000, spentCents = 85_000, warningPercentage = 80)
        assertEquals(35_000L, p.remainingCents)
        assertEquals(70, p.percentage)
        assertEquals(BudgetState.AVAILABLE, p.state)
    }

    @Test
    fun warningAtOrAboveThreshold() {
        assertEquals(BudgetState.WARNING, BudgetCalculator.progress(100_000, 80_000, 80).state)
        assertEquals(BudgetState.WARNING, BudgetCalculator.progress(120_000, 100_000, 80).state)
        // exactamente en el limite sigue siendo WARNING, no superado
        assertEquals(BudgetState.WARNING, BudgetCalculator.progress(100_000, 100_000, 80).state)
    }

    @Test
    fun exceededAboveLimit_withNegativeRemaining() {
        val p = BudgetCalculator.progress(120_000, 130_000, 80)
        assertEquals(BudgetState.EXCEEDED, p.state)
        assertEquals(-10_000L, p.remainingCents)
        assertEquals(108, p.percentage)
    }

    @Test
    fun zeroLimitDoesNotDivideByZero() {
        val p = BudgetCalculator.progress(0, 100, 80)
        assertEquals(0, p.percentage)
        assertEquals(BudgetState.EXCEEDED, p.state)
    }
}
