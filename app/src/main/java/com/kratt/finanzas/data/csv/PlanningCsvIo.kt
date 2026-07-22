package com.kratt.finanzas.data.csv

import androidx.room.withTransaction
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.entity.PlannedPurchaseEntity
import com.kratt.finanzas.data.local.entity.SavingsGoalEntity
import com.kratt.finanzas.data.report.CsvCell
import com.kratt.finanzas.data.report.CsvTable
import com.kratt.finanzas.domain.model.PlannedPurchase
import com.kratt.finanzas.domain.model.PurchasePriority
import com.kratt.finanzas.domain.model.PurchaseStatus
import com.kratt.finanzas.domain.model.SavingsGoal
import com.kratt.finanzas.domain.model.SavingsGoalStatus
import com.kratt.finanzas.domain.usecase.csv.CsvImportParser
import com.kratt.finanzas.domain.usecase.csv.PlanningCsvFormat
import com.kratt.finanzas.domain.usecase.csv.PlanningCsvValidator
import com.kratt.finanzas.domain.usecase.csv.PlanningImportKind
import com.kratt.finanzas.domain.usecase.csv.PlanningImportPreview
import java.io.InputStream
import java.time.LocalDate
import kotlinx.coroutines.flow.first

// importa y exporta metas y compras planificadas en csv; nunca crea movimientos financieros
class PlanningCsvImporter(
    private val database: AppDatabase,
    private val savingsGoalRepository: com.kratt.finanzas.data.repository.SavingsGoalRepository,
) {

    private val goalDao = database.savingsGoalDao()
    private val purchaseDao = database.plannedPurchaseDao()

    // lee y analiza el archivo, con un tope de tamano antes de reservar memoria
    suspend fun preview(input: InputStream, today: LocalDate = LocalDate.now()): PlanningImportPreview {
        val bytes = input.buffered().readBytes().let { if (it.size > MAX_SIZE) return PlanningImportPreview(fileError = true) else it }
        val text = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull() ?: return PlanningImportPreview(fileError = true)
        val parsed = CsvImportParser.parse(text)
        val existing = savingsGoalRepository.observeAll().first().map { it.name }.toSet()
        return PlanningCsvValidator.buildPreview(parsed, existing, today)
    }

    // inserta todas las filas validas en una sola operacion; si algo falla no queda nada a medias
    suspend fun commit(preview: PlanningImportPreview, today: LocalDate = LocalDate.now()): Int = database.withTransaction {
        val now = System.currentTimeMillis()
        when (preview.kind) {
            PlanningImportKind.GOALS -> {
                preview.validGoals.forEach { g ->
                    goalDao.insert(
                        SavingsGoalEntity(
                            name = g.name, targetAmountCents = g.targetAmountCents, linkedAccountId = null,
                            startDate = today.toEpochDay(), targetDate = g.targetDate?.toEpochDay(),
                            status = SavingsGoalStatus.ACTIVE, description = g.description, iconKey = "savings",
                            colorKey = "green", isArchived = false, createdAt = now, updatedAt = now,
                        ),
                    )
                }
                preview.validGoals.size
            }
            PlanningImportKind.PURCHASES -> {
                preview.validPurchases.forEach { p ->
                    purchaseDao.insert(
                        PlannedPurchaseEntity(
                            name = p.name, estimatedCostCents = p.estimatedCostCents, categoryId = null,
                            savingsGoalId = null, targetDate = p.targetDate?.toEpochDay(), priority = p.priority,
                            status = PurchaseStatus.PLANNING, description = p.description, vendor = null,
                            purchasedTransactionId = null, createdAt = now, updatedAt = now,
                        ),
                    )
                }
                preview.validPurchases.size
            }
            PlanningImportKind.UNKNOWN -> 0
        }
    }

    private companion object {
        const val MAX_SIZE = 5 * 1024 * 1024
    }
}

// arma las tablas csv de exportacion de planificacion, en formatos separados y documentados
object PlanningCsvExporter {

    fun goalsTable(goals: List<SavingsGoal>, totals: Map<Long, Long>): CsvTable {
        val header = PlanningCsvFormat.GOAL_HEADER
        val rows = goals.map { g ->
            listOf(
                CsvCell.Text(g.name),
                CsvCell.Value(AmountParser.formatCents(g.targetAmountCents)),
                CsvCell.Text(g.targetDate?.toString() ?: ""),
                CsvCell.Text(g.description ?: ""),
            )
        }
        return CsvTable(header, rows)
    }

    fun purchasesTable(purchases: List<PlannedPurchase>): CsvTable {
        val header = PlanningCsvFormat.PURCHASE_HEADER
        val rows = purchases.map { p ->
            listOf(
                CsvCell.Text(p.name),
                CsvCell.Value(AmountParser.formatCents(p.estimatedCostCents)),
                CsvCell.Text(priorityText(p.priority)),
                CsvCell.Text(p.targetDate?.toString() ?: ""),
                CsvCell.Text(p.description ?: ""),
            )
        }
        return CsvTable(header, rows)
    }

    private fun priorityText(priority: PurchasePriority): String = when (priority) {
        PurchasePriority.LOW -> "baja"
        PurchasePriority.MEDIUM -> "media"
        PurchasePriority.HIGH -> "alta"
    }
}
