package com.kratt.finanzas.domain.usecase.csv

import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate

// columnas oficiales del formato de importacion
object CsvImportFormat {
    const val COL_TYPE = "tipo"
    const val COL_DATE = "fecha"
    const val COL_DESCRIPTION = "descripcion"
    const val COL_AMOUNT = "monto"
    const val COL_ACCOUNT = "cuenta"
    const val COL_CATEGORY = "categoria"
    const val COL_DESTINATION = "cuenta_destino"

    // columnas obligatorias en el encabezado; cuenta_destino es opcional (solo transferencias)
    val REQUIRED = listOf(COL_TYPE, COL_DATE, COL_DESCRIPTION, COL_AMOUNT, COL_ACCOUNT, COL_CATEGORY)

    val VALUE_EXPENSE = "gasto"
    val VALUE_INCOME = "ingreso"
    val VALUE_TRANSFER = "transferencia"

    // tamano maximo del archivo para no cargar algo gigantesco en memoria
    const val MAX_FILE_BYTES = 5L * 1024 * 1024
}

// error de una sola fila; cada uno mapea a un mensaje en espanol en la ui
enum class ImportRowError {
    MALFORMED_ROW,
    INVALID_TYPE,
    INVALID_DATE,
    INVALID_AMOUNT,
    AMOUNT_TOO_LARGE,
    ACCOUNT_NOT_FOUND,
    MISSING_CATEGORY,
    CATEGORY_NOT_FOUND,
    MISSING_DESTINATION,
    DESTINATION_NOT_FOUND,
    SAME_ACCOUNT,
}

// error del archivo completo que impide continuar
enum class ImportFileError {
    EMPTY,
    MISSING_REQUIRED_COLUMN,
    TOO_LARGE,
}

// una fila cruda del csv con su numero (1-based, sin contar el encabezado)
data class RawCsvRow(val rowNumber: Int, val values: List<String>)

// un movimiento ya resuelto contra cuentas y categorias, listo para insertar
data class ImportedMovement(
    val rowNumber: Int,
    val type: TransactionType,
    val date: LocalDate,
    val description: String?,
    val amountCents: Long,
    val accountId: Long,
    val categoryId: Long?,
    val destinationAccountId: Long?,
)

// resultado de validar una fila
sealed interface RowResult {
    data class Valid(val movement: ImportedMovement) : RowResult
    data class Invalid(val rowNumber: Int, val error: ImportRowError) : RowResult
}

// vista previa completa de la importacion
data class ImportPreview(
    val fileError: ImportFileError? = null,
    val valid: List<ImportedMovement> = emptyList(),
    val duplicates: List<ImportedMovement> = emptyList(),
    val errors: List<RowResult.Invalid> = emptyList(),
) {
    val canImport: Boolean get() = fileError == null && valid.isNotEmpty()
    val validCount: Int get() = valid.size
    val duplicateCount: Int get() = duplicates.size
    val errorCount: Int get() = errors.size
}

// resumen final despues de importar
data class ImportSummary(
    val imported: Int,
    val skippedDuplicates: Int,
    val errors: Int,
    val duplicatesDetected: Int,
)
