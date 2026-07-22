package com.kratt.finanzas.domain.usecase.csv

import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.domain.model.PurchasePriority
import com.kratt.finanzas.domain.usecase.MoneyMath
import com.kratt.finanzas.domain.usecase.TextNormalizer
import java.time.LocalDate

// tipo de archivo de planificacion detectado por el encabezado
enum class PlanningImportKind { GOALS, PURCHASES, UNKNOWN }

// formatos oficiales de importacion de planificacion
object PlanningCsvFormat {
    val GOAL_HEADER = listOf("nombre", "monto_objetivo", "fecha_objetivo", "descripcion")
    val PURCHASE_HEADER = listOf("nombre", "costo_estimado", "prioridad", "fecha_estimada", "descripcion")

    fun detect(header: List<String>): PlanningImportKind {
        val h = header.map { it.trim().lowercase() }
        return when {
            h.contains("monto_objetivo") -> PlanningImportKind.GOALS
            h.contains("costo_estimado") -> PlanningImportKind.PURCHASES
            else -> PlanningImportKind.UNKNOWN
        }
    }
}

// meta lista para insertar tras validar
data class ParsedGoalRow(
    val name: String,
    val targetAmountCents: Long,
    val targetDate: LocalDate?,
    val description: String?,
)

// compra lista para insertar tras validar
data class ParsedPurchaseRow(
    val name: String,
    val estimatedCostCents: Long,
    val priority: PurchasePriority,
    val targetDate: LocalDate?,
    val description: String?,
)

// resultado del analisis: filas validas, con error y duplicadas por numero de fila
data class PlanningImportPreview(
    val kind: PlanningImportKind = PlanningImportKind.UNKNOWN,
    val fileError: Boolean = false,
    val validGoals: List<ParsedGoalRow> = emptyList(),
    val validPurchases: List<ParsedPurchaseRow> = emptyList(),
    val errorRows: List<Int> = emptyList(),
    val duplicateRows: List<Int> = emptyList(),
) {
    val validCount: Int get() = validGoals.size + validPurchases.size
    val hasImportable: Boolean get() = validCount > 0
}

// valida las filas de un csv de planificacion; el nombre duplicado se revisa contra los existentes
object PlanningCsvValidator {

    fun buildPreview(parsed: ParsedCsv, existingGoalNames: Set<String>, today: LocalDate): PlanningImportPreview {
        if (parsed.header.isEmpty() || parsed.rows.isEmpty()) return PlanningImportPreview(fileError = true)
        val kind = PlanningCsvFormat.detect(parsed.header)
        return when (kind) {
            PlanningImportKind.GOALS -> goalsPreview(parsed, existingGoalNames, today)
            PlanningImportKind.PURCHASES -> purchasesPreview(parsed)
            PlanningImportKind.UNKNOWN -> PlanningImportPreview(fileError = true)
        }
    }

    private fun goalsPreview(parsed: ParsedCsv, existingNames: Set<String>, today: LocalDate): PlanningImportPreview {
        val index = columnIndex(parsed.header)
        val existingNormalized = existingNames.map { TextNormalizer.normalize(it) }.toMutableSet()
        val valid = mutableListOf<ParsedGoalRow>()
        val errors = mutableListOf<Int>()
        val duplicates = mutableListOf<Int>()
        parsed.rows.forEach { row ->
            val name = row.value(index["nombre"]).trim()
            val amount = AmountParser.parseToCents(row.value(index["monto_objetivo"]))
            val date = parseDate(row.value(index["fecha_objetivo"]))
            val dateBad = row.value(index["fecha_objetivo"]).isNotBlank() && date == null
            when {
                name.isBlank() || amount == null || amount <= 0 || amount > MoneyMath.MAX_SUPPORTED_CENTS -> errors += row.rowNumber
                dateBad || (date != null && !date.isAfter(today)) -> errors += row.rowNumber
                TextNormalizer.normalize(name) in existingNormalized -> duplicates += row.rowNumber
                else -> {
                    existingNormalized += TextNormalizer.normalize(name)
                    valid += ParsedGoalRow(name, amount, date, row.value(index["descripcion"]).trim().ifBlank { null })
                }
            }
        }
        return PlanningImportPreview(PlanningImportKind.GOALS, validGoals = valid, errorRows = errors, duplicateRows = duplicates)
    }

    private fun purchasesPreview(parsed: ParsedCsv): PlanningImportPreview {
        val index = columnIndex(parsed.header)
        val valid = mutableListOf<ParsedPurchaseRow>()
        val errors = mutableListOf<Int>()
        parsed.rows.forEach { row ->
            val name = row.value(index["nombre"]).trim()
            val amount = AmountParser.parseToCents(row.value(index["costo_estimado"]))
            val priority = parsePriority(row.value(index["prioridad"]))
            val date = parseDate(row.value(index["fecha_estimada"]))
            val dateBad = row.value(index["fecha_estimada"]).isNotBlank() && date == null
            when {
                name.isBlank() || amount == null || amount <= 0 || amount > MoneyMath.MAX_SUPPORTED_CENTS -> errors += row.rowNumber
                priority == null || dateBad -> errors += row.rowNumber
                else -> valid += ParsedPurchaseRow(name, amount, priority, date, row.value(index["descripcion"]).trim().ifBlank { null })
            }
        }
        return PlanningImportPreview(PlanningImportKind.PURCHASES, validPurchases = valid, errorRows = errors)
    }

    private fun columnIndex(header: List<String>): Map<String, Int> =
        header.mapIndexed { i, h -> h.trim().lowercase() to i }.toMap()

    private fun RawCsvRow.value(index: Int?): String = if (index != null && index < values.size) values[index] else ""

    private fun parseDate(text: String): LocalDate? =
        if (text.isBlank()) null else runCatching { LocalDate.parse(text.trim()) }.getOrNull()

    private fun parsePriority(text: String): PurchasePriority? = when (text.trim().lowercase()) {
        "baja" -> PurchasePriority.LOW
        "media" -> PurchasePriority.MEDIUM
        "alta" -> PurchasePriority.HIGH
        else -> null
    }
}
