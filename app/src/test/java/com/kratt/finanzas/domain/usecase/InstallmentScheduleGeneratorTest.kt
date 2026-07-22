package com.kratt.finanzas.domain.usecase

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallmentScheduleGeneratorTest {

    @Test
    fun generatesSequentialMonthlyDueDates() {
        val schedule = InstallmentScheduleGenerator.generate(LocalDate.of(2026, 3, 15), 3, 3_000)
        assertEquals(listOf(1, 2, 3), schedule.map { it.sequenceNumber })
        assertEquals(
            listOf(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 4, 15), LocalDate.of(2026, 5, 15)),
            schedule.map { it.dueDate },
        )
        assertEquals(3_000L, schedule.sumOf { it.amountCents })
    }

    @Test
    fun monthEndStrategyClampsAndReturnsToAnchorDay() {
        // empieza el 31 de enero: febrero se recorta, marzo vuelve al 31
        val schedule = InstallmentScheduleGenerator.generate(LocalDate.of(2026, 1, 31), 4, 4_000)
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
            ),
            schedule.map { it.dueDate },
        )
    }

    @Test
    fun distributionSumMatchesTotal() {
        val schedule = InstallmentScheduleGenerator.generate(LocalDate.of(2026, 1, 10), 7, 1_000)
        assertEquals(1_000L, schedule.sumOf { it.amountCents })
    }
}
