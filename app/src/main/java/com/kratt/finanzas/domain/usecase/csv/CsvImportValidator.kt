package com.kratt.finanzas.domain.usecase.csv

import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.MoneyMath
import com.kratt.finanzas.domain.usecase.TextNormalizer
import java.time.LocalDate
import java.time.format.DateTimeParseException

// referencia de cuentas y categorias activas y de los movimientos ya guardados
data class ImportContext(
    val accountsByName: Map<String, Long>,
    val expenseCategoriesByName: Map<String, Long>,
    val incomeCategoriesByName: Map<String, Long>,
    val existingKeys: Set<String>,
)

// valida el csv completo y arma la vista previa con validos, duplicados y errores
object CsvImportValidator {

    fun buildPreview(parsed: ParsedCsv, context: ImportContext): ImportPreview {
        if (parsed.header.isEmpty()) return ImportPreview(fileError = ImportFileError.EMPTY)
        val index = parsed.header.withIndex().associate { (i, name) -> name to i }
        if (CsvImportFormat.REQUIRED.any { it !in index }) {
            return ImportPreview(fileError = ImportFileError.MISSING_REQUIRED_COLUMN)
        }
        if (parsed.rows.isEmpty()) return ImportPreview(fileError = ImportFileError.EMPTY)

        val valid = ArrayList<ImportedMovement>()
        val duplicates = ArrayList<ImportedMovement>()
        val errors = ArrayList<RowResult.Invalid>()
        // arranca con las llaves ya existentes para marcar duplicados contra la base y dentro del archivo
        val seen = HashSet(context.existingKeys)

        for (row in parsed.rows) {
            when (val result = validateRow(row, index, context)) {
                is RowResult.Invalid -> errors.add(result)
                is RowResult.Valid -> {
                    valid.add(result.movement)
                    if (!seen.add(duplicateKey(result.movement))) duplicates.add(result.movement)
                }
            }
        }
        return ImportPreview(valid = valid, duplicates = duplicates, errors = errors)
    }

    // llave para detectar posibles duplicados; descripcion normalizada sin acentos ni mayusculas
    fun duplicateKey(movement: ImportedMovement): String = keyOf(
        movement.type, movement.date, movement.amountCents, movement.accountId,
        movement.categoryId, movement.destinationAccountId, movement.description,
    )

    // misma llave calculada desde los campos crudos, sirve para movimientos ya guardados
    fun keyOf(
        type: TransactionType,
        date: LocalDate,
        amountCents: Long,
        accountId: Long,
        categoryId: Long?,
        destinationAccountId: Long?,
        description: String?,
    ): String = listOf(
        type.name,
        date.toString(),
        amountCents.toString(),
        accountId.toString(),
        (categoryId ?: 0L).toString(),
        (destinationAccountId ?: 0L).toString(),
        TextNormalizer.normalize(description ?: ""),
    ).joinToString("|")

    private fun cell(row: RawCsvRow, index: Map<String, Int>, name: String): String? =
        index[name]?.let { i -> row.values.getOrNull(i)?.trim() }

    private fun validateRow(row: RawCsvRow, index: Map<String, Int>, context: ImportContext): RowResult {
        val typeRaw = cell(row, index, CsvImportFormat.COL_TYPE)
            ?: return RowResult.Invalid(row.rowNumber, ImportRowError.MALFORMED_ROW)
        val type = when (TextNormalizer.normalize(typeRaw)) {
            CsvImportFormat.VALUE_EXPENSE -> TransactionType.EXPENSE
            CsvImportFormat.VALUE_INCOME -> TransactionType.INCOME
            CsvImportFormat.VALUE_TRANSFER -> TransactionType.TRANSFER
            else -> return RowResult.Invalid(row.rowNumber, ImportRowError.INVALID_TYPE)
        }

        val dateRaw = cell(row, index, CsvImportFormat.COL_DATE)
        val date = parseIsoDate(dateRaw) ?: return RowResult.Invalid(row.rowNumber, ImportRowError.INVALID_DATE)

        val amount = cell(row, index, CsvImportFormat.COL_AMOUNT)?.let { AmountParser.parseToCents(it) }
            ?: return RowResult.Invalid(row.rowNumber, ImportRowError.INVALID_AMOUNT)
        if (!MoneyMath.isSupportedAmount(amount)) return RowResult.Invalid(row.rowNumber, ImportRowError.AMOUNT_TOO_LARGE)

        val accountName = cell(row, index, CsvImportFormat.COL_ACCOUNT)?.takeIf { it.isNotBlank() }
            ?: return RowResult.Invalid(row.rowNumber, ImportRowError.ACCOUNT_NOT_FOUND)
        val accountId = context.accountsByName[TextNormalizer.normalize(accountName)]
            ?: return RowResult.Invalid(row.rowNumber, ImportRowError.ACCOUNT_NOT_FOUND)

        val description = cell(row, index, CsvImportFormat.COL_DESCRIPTION)?.takeIf { it.isNotBlank() }

        return when (type) {
            TransactionType.TRANSFER -> {
                val destName = cell(row, index, CsvImportFormat.COL_DESTINATION)?.takeIf { it.isNotBlank() }
                    ?: return RowResult.Invalid(row.rowNumber, ImportRowError.MISSING_DESTINATION)
                val destId = context.accountsByName[TextNormalizer.normalize(destName)]
                    ?: return RowResult.Invalid(row.rowNumber, ImportRowError.DESTINATION_NOT_FOUND)
                if (destId == accountId) return RowResult.Invalid(row.rowNumber, ImportRowError.SAME_ACCOUNT)
                RowResult.Valid(ImportedMovement(row.rowNumber, type, date, description, amount, accountId, null, destId))
            }
            else -> {
                val categoryName = cell(row, index, CsvImportFormat.COL_CATEGORY)?.takeIf { it.isNotBlank() }
                    ?: return RowResult.Invalid(row.rowNumber, ImportRowError.MISSING_CATEGORY)
                val categories = if (type == TransactionType.EXPENSE) context.expenseCategoriesByName else context.incomeCategoriesByName
                val categoryId = categories[TextNormalizer.normalize(categoryName)]
                    ?: return RowResult.Invalid(row.rowNumber, ImportRowError.CATEGORY_NOT_FOUND)
                RowResult.Valid(ImportedMovement(row.rowNumber, type, date, description, amount, accountId, categoryId, null))
            }
        }
    }

    // solo acepta fechas iso yyyy-mm-dd para evitar ambiguedad de formatos
    private fun parseIsoDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
