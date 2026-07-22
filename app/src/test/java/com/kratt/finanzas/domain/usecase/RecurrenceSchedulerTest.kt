package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.RecurrenceType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceSchedulerTest {

    private val horizon = LocalDate.of(2027, 12, 31)

    @Test
    fun weeklyGeneration() {
        val dates = RecurrenceScheduler.scheduledDates(
            LocalDate.of(2026, 1, 1), RecurrenceType.WEEKLY, 1, null, LocalDate.of(2026, 1, 29),
        )
        assertEquals(
            listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 22), LocalDate.of(2026, 1, 29)),
            dates,
        )
    }

    @Test
    fun monthlyGenerationAnchorsToStartDay() {
        val dates = RecurrenceScheduler.scheduledDates(
            LocalDate.of(2026, 1, 31), RecurrenceType.MONTHLY, 1, null, LocalDate.of(2026, 4, 30),
        )
        assertEquals(
            listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30)),
            dates,
        )
    }

    @Test
    fun yearlyGenerationHandlesLeapDay() {
        val dates = RecurrenceScheduler.scheduledDates(
            LocalDate.of(2024, 2, 29), RecurrenceType.YEARLY, 1, null, LocalDate.of(2026, 12, 31),
        )
        assertEquals(
            listOf(LocalDate.of(2024, 2, 29), LocalDate.of(2025, 2, 28), LocalDate.of(2026, 2, 28)),
            dates,
        )
    }

    @Test
    fun intervalGreaterThanOne() {
        val dates = RecurrenceScheduler.scheduledDates(
            LocalDate.of(2026, 1, 15), RecurrenceType.MONTHLY, 3, null, LocalDate.of(2026, 12, 31),
        )
        assertEquals(
            listOf(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 4, 15), LocalDate.of(2026, 7, 15), LocalDate.of(2026, 10, 15)),
            dates,
        )
    }

    @Test
    fun endDateStopsGeneration() {
        val dates = RecurrenceScheduler.scheduledDates(
            LocalDate.of(2026, 1, 1), RecurrenceType.MONTHLY, 1, LocalDate.of(2026, 3, 1), horizon,
        )
        assertEquals(3, dates.size)
        assertEquals(LocalDate.of(2026, 3, 1), dates.last())
    }

    @Test
    fun capBoundsTheWindow() {
        val dates = RecurrenceScheduler.scheduledDates(
            LocalDate.of(2026, 1, 1), RecurrenceType.WEEKLY, 1, null, horizon, cap = 5,
        )
        assertEquals(5, dates.size)
        assertTrue(dates.all { !it.isAfter(horizon) })
    }
}
