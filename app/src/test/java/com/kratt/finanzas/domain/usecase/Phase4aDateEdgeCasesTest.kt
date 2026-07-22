package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.RecurrenceType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

// verifica el manejo de fin de mes, anos bisiestos y cruce de ano en las recurrencias
class Phase4aDateEdgeCasesTest {

    private fun advance(start: LocalDate, type: RecurrenceType, steps: Int) =
        RecurrenceScheduler.advance(start, type, interval = 1, steps = steps)

    @Test
    fun monthly_fromJan31_clampsToShortMonths_butKeepsAnchor() {
        val start = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28), advance(start, RecurrenceType.MONTHLY, 1))
        // el ancla del 31 se conserva para los meses que si lo tienen
        assertEquals(LocalDate.of(2026, 3, 31), advance(start, RecurrenceType.MONTHLY, 2))
        assertEquals(LocalDate.of(2026, 4, 30), advance(start, RecurrenceType.MONTHLY, 3))
    }

    @Test
    fun monthly_leapYearFebruary_uses29() {
        val start = LocalDate.of(2024, 1, 31)
        assertEquals(LocalDate.of(2024, 2, 29), advance(start, RecurrenceType.MONTHLY, 1))
    }

    @Test
    fun yearly_fromFeb29_clampsInNonLeapYears() {
        val start = LocalDate.of(2024, 2, 29)
        assertEquals(LocalDate.of(2025, 2, 28), advance(start, RecurrenceType.YEARLY, 1))
        assertEquals(LocalDate.of(2028, 2, 29), advance(start, RecurrenceType.YEARLY, 4))
    }

    @Test
    fun weekly_crossesYearBoundary() {
        val start = LocalDate.of(2026, 12, 20)
        assertEquals(LocalDate.of(2027, 1, 3), advance(start, RecurrenceType.WEEKLY, 2))
    }

    @Test
    fun customRange_validation() {
        assertEquals(true, DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)).isValid)
        // mismo dia es un rango valido de un solo dia
        assertEquals(true, DateRange(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 10)).isValid)
        // inicio despues del fin es invalido
        assertEquals(false, DateRange(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 1)).isValid)
    }
}
