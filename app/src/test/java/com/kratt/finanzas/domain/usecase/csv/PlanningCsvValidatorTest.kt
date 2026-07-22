package com.kratt.finanzas.domain.usecase.csv

import com.kratt.finanzas.domain.model.PurchasePriority
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanningCsvValidatorTest {

    private val today = LocalDate.of(2026, 1, 1)
    private val goalHeader = listOf("nombre", "monto_objetivo", "fecha_objetivo", "descripcion")
    private val purchaseHeader = listOf("nombre", "costo_estimado", "prioridad", "fecha_estimada", "descripcion")

    private fun row(n: Int, vararg v: String) = RawCsvRow(n, v.toList())

    @Test
    fun detectsGoalsAndPurchases() {
        assertEquals(PlanningImportKind.GOALS, PlanningCsvFormat.detect(goalHeader))
        assertEquals(PlanningImportKind.PURCHASES, PlanningCsvFormat.detect(purchaseHeader))
        assertEquals(PlanningImportKind.UNKNOWN, PlanningCsvFormat.detect(listOf("a", "b")))
    }

    @Test
    fun goals_validRowParsed() {
        val parsed = ParsedCsv(goalHeader, listOf(row(1, "Fondo", "5000.00", "2027-01-01", "emergencia")))
        val preview = PlanningCsvValidator.buildPreview(parsed, emptySet(), today)
        assertEquals(PlanningImportKind.GOALS, preview.kind)
        assertEquals(1, preview.validGoals.size)
        assertEquals(500_000L, preview.validGoals[0].targetAmountCents)
        assertEquals(LocalDate.of(2027, 1, 1), preview.validGoals[0].targetDate)
    }

    @Test
    fun goals_blankNameAndBadAmountAndPastDate_areErrors() {
        val parsed = ParsedCsv(
            goalHeader,
            listOf(
                row(1, "  ", "5000.00", "", ""),
                row(2, "Meta", "abc", "", ""),
                row(3, "Meta2", "1000.00", "2025-01-01", ""),
            ),
        )
        val preview = PlanningCsvValidator.buildPreview(parsed, emptySet(), today)
        assertEquals(listOf(1, 2, 3), preview.errorRows)
        assertTrue(preview.validGoals.isEmpty())
    }

    @Test
    fun goals_duplicateNames_flaggedAgainstExistingAndWithinFile() {
        val parsed = ParsedCsv(
            goalHeader,
            listOf(
                row(1, "Vacaciones", "1000.00", "", ""),
                row(2, "vacaciones", "2000.00", "", ""),
            ),
        )
        // ya existe una meta con el mismo nombre normalizado
        val preview = PlanningCsvValidator.buildPreview(parsed, setOf("Vacaciones"), today)
        assertEquals(listOf(1, 2), preview.duplicateRows)
        assertTrue(preview.validGoals.isEmpty())
    }

    @Test
    fun purchases_validAndBadPriority() {
        val parsed = ParsedCsv(
            purchaseHeader,
            listOf(
                row(1, "Laptop", "8000.00", "alta", "2026-06-01", ""),
                row(2, "Tele", "3000.00", "urgente", "", ""),
            ),
        )
        val preview = PlanningCsvValidator.buildPreview(parsed, emptySet(), today)
        assertEquals(1, preview.validPurchases.size)
        assertEquals(PurchasePriority.HIGH, preview.validPurchases[0].priority)
        assertEquals(800_000L, preview.validPurchases[0].estimatedCostCents)
        assertEquals(listOf(2), preview.errorRows)
    }

    @Test
    fun emptyFile_isFileError() {
        val preview = PlanningCsvValidator.buildPreview(ParsedCsv(goalHeader, emptyList()), emptySet(), today)
        assertTrue(preview.fileError)
    }
}
