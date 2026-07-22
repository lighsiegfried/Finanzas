package com.kratt.finanzas.domain.usecase

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalProgressCalculatorTest {

    private val start = LocalDate.of(2026, 1, 1)
    private val today = LocalDate.of(2026, 4, 1)

    private fun points(vararg amounts: Long) = amounts.map { ContributionPoint(it, LocalDate.of(2026, 2, 1)) }

    @Test
    fun contributedRemainingAndPercent() {
        val p = GoalProgressCalculator.calculate(500_000, points(100_000, 100_000), start, LocalDate.of(2026, 7, 1), today)
        assertEquals(200_000, p.contributedCents)
        assertEquals(300_000, p.remainingCents)
        assertEquals(40, p.progressPercent)
        assertFalse(p.isComplete)
        assertEquals(0, p.surplusCents)
    }

    @Test
    fun completedAndSurplus() {
        val p = GoalProgressCalculator.calculate(300_000, points(200_000, 150_000), start, null, today)
        assertTrue(p.isComplete)
        assertEquals(0, p.remainingCents)
        assertEquals(50_000, p.surplusCents)
    }

    @Test
    fun remainingNeverNegative() {
        val p = GoalProgressCalculator.calculate(100_000, points(250_000), start, null, today)
        assertEquals(0, p.remainingCents)
        assertTrue(p.isComplete)
    }

    @Test
    fun suggestedMonthlyDistributesRemainingCeil() {
        // faltan 300000 en 3 meses -> 100000 por mes
        val p = GoalProgressCalculator.calculate(500_000, points(100_000, 100_000), start, LocalDate.of(2026, 7, 1), today)
        assertEquals(100_000L, p.suggestedMonthlyCents)
    }

    @Test
    fun suggestedMonthlyRoundsUpAndNeverLosesCents() {
        // faltan 100000 en 3 meses -> 33334 (redondeo hacia arriba, la suma cubre el total)
        val p = GoalProgressCalculator.calculate(100_000, emptyList(), start, LocalDate.of(2026, 7, 1), today)
        assertEquals(33_334L, p.suggestedMonthlyCents)
        assertTrue(p.suggestedMonthlyCents!! * 3 >= 100_000)
    }

    @Test
    fun noTargetDateHasNoSuggestedMonthly() {
        val p = GoalProgressCalculator.calculate(500_000, points(100_000), start, null, today)
        assertNull(p.suggestedMonthlyCents)
        assertFalse(p.hasTargetDate)
    }

    @Test
    fun averageMonthlyFromHistory() {
        // 200000 en 4 meses transcurridos -> 50000 promedio
        val p = GoalProgressCalculator.calculate(500_000, points(100_000, 100_000), start, null, today)
        assertEquals(50_000L, p.averageMonthlyCents)
    }

    @Test
    fun estimatedDateFromAverage() {
        val p = GoalProgressCalculator.calculate(500_000, points(100_000, 100_000), start, null, today)
        // faltan 300000 a 50000/mes -> 6 meses desde hoy
        assertEquals(today.plusMonths(6), p.estimatedDate)
    }

    @Test
    fun noHistoryHasNoAverageOrEstimate() {
        val p = GoalProgressCalculator.calculate(500_000, emptyList(), start, null, today)
        assertNull(p.averageMonthlyCents)
        assertNull(p.estimatedDate)
    }
}
