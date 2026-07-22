package com.kratt.finanzas.domain.usecase.csv

import com.kratt.finanzas.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImportTest {

    private val header = "tipo,fecha,descripcion,monto,cuenta,categoria,cuenta_destino"

    private fun context() = ImportContext(
        accountsByName = mapOf("efectivo" to 1L, "ahorro" to 2L, "banco" to 3L),
        expenseCategoriesByName = mapOf("alimentacion" to 10L, "transporte" to 11L),
        incomeCategoriesByName = mapOf("salario" to 20L),
        existingKeys = emptySet(),
    )

    private fun preview(csv: String, ctx: ImportContext = context()): ImportPreview =
        CsvImportValidator.buildPreview(CsvImportParser.parse(csv), ctx)

    // --- parser ---

    @Test
    fun parser_handlesQuotedFieldsWithCommasAndCrlf() {
        val csv = "$header\r\ngasto,2026-07-10,\"Pago, urgente\",100.50,Efectivo,Alimentacion,\r\n"
        val parsed = CsvImportParser.parse(csv)
        assertEquals(7, parsed.header.size)
        assertEquals(1, parsed.rows.size)
        assertEquals("Pago, urgente", parsed.rows[0].values[2])
    }

    @Test
    fun parser_stripsBom_andIgnoresBlankLines() {
        val csv = "﻿$header\ningreso,2026-07-01,Salario,8000.00,Efectivo,Salario,\n\n"
        val parsed = CsvImportParser.parse(csv)
        assertEquals("tipo", parsed.header[0])
        assertEquals(1, parsed.rows.size)
    }

    // --- valid rows ---

    @Test
    fun validExpense_isAccepted_withAccentAndCaseTolerantMatching() {
        val p = preview("$header\nGASTO,2026-07-10,Almuerzo,85.00,efectivo,ALIMENTACIÓN,")
        assertEquals(1, p.validCount)
        val m = p.valid.first()
        assertEquals(TransactionType.EXPENSE, m.type)
        assertEquals(8500L, m.amountCents)
        assertEquals(1L, m.accountId)
        assertEquals(10L, m.categoryId)
        assertNull(m.destinationAccountId)
    }

    @Test
    fun validTransfer_isAccepted_withDestination() {
        val p = preview("$header\ntransferencia,2026-07-11,Ahorro mensual,500.00,Efectivo,,Ahorro")
        assertEquals(1, p.validCount)
        val m = p.valid.first()
        assertEquals(TransactionType.TRANSFER, m.type)
        assertEquals(1L, m.accountId)
        assertEquals(2L, m.destinationAccountId)
        assertNull(m.categoryId)
    }

    // --- errors ---

    @Test
    fun missingRequiredColumn_isFileError() {
        val p = preview("tipo,fecha,descripcion,monto,cuenta\ngasto,2026-07-10,x,10.00,Efectivo")
        assertEquals(ImportFileError.MISSING_REQUIRED_COLUMN, p.fileError)
        assertTrue(!p.canImport)
    }

    @Test
    fun emptyFile_isFileError() {
        assertEquals(ImportFileError.EMPTY, preview(header).fileError)
    }

    @Test
    fun invalidRows_reportPreciseErrors() {
        val csv = buildString {
            appendLine(header)
            appendLine("comida,2026-07-10,x,10.00,Efectivo,Alimentacion,")      // tipo invalido
            appendLine("gasto,10/07/2026,x,10.00,Efectivo,Alimentacion,")        // fecha invalida
            appendLine("gasto,2026-07-10,x,-5.00,Efectivo,Alimentacion,")        // monto invalido
            appendLine("gasto,2026-07-10,x,10.00,Cartera,Alimentacion,")         // cuenta no existe
            appendLine("gasto,2026-07-10,x,10.00,Efectivo,Regalos,")            // categoria no existe
            appendLine("transferencia,2026-07-10,x,10.00,Efectivo,,Efectivo")   // misma cuenta
            appendLine("transferencia,2026-07-10,x,10.00,Efectivo,,")           // falta destino
        }
        val p = preview(csv)
        val byError = p.errors.associate { it.rowNumber to it.error }
        assertEquals(ImportRowError.INVALID_TYPE, byError[1])
        assertEquals(ImportRowError.INVALID_DATE, byError[2])
        assertEquals(ImportRowError.INVALID_AMOUNT, byError[3])
        assertEquals(ImportRowError.ACCOUNT_NOT_FOUND, byError[4])
        assertEquals(ImportRowError.CATEGORY_NOT_FOUND, byError[5])
        assertEquals(ImportRowError.SAME_ACCOUNT, byError[6])
        assertEquals(ImportRowError.MISSING_DESTINATION, byError[7])
        assertEquals(0, p.validCount)
    }

    // --- duplicates ---

    @Test
    fun duplicateWithinFile_isFlagged() {
        val row = "gasto,2026-07-10,Almuerzo,85.00,Efectivo,Alimentacion,"
        val p = preview("$header\n$row\n$row")
        assertEquals(2, p.validCount)
        assertEquals(1, p.duplicateCount)
    }

    @Test
    fun duplicateAgainstExisting_isFlagged() {
        val movement = ImportedMovement(1, TransactionType.EXPENSE, java.time.LocalDate.of(2026, 7, 10), "Almuerzo", 8500L, 1L, 10L, null)
        val ctx = context().copy(existingKeys = setOf(CsvImportValidator.duplicateKey(movement)))
        val p = preview("$header\ngasto,2026-07-10,Almuerzo,85.00,Efectivo,Alimentacion,", ctx)
        assertEquals(1, p.validCount)
        assertEquals(1, p.duplicateCount)
    }

    @Test
    fun transferImport_isNeutralToIncomeAndExpense() {
        // una transferencia importada no debe tener categoria y usa dos cuentas distintas
        val m = preview("$header\ntransferencia,2026-07-11,x,500.00,Efectivo,,Ahorro").valid.first()
        assertEquals(TransactionType.TRANSFER, m.type)
        assertNull(m.categoryId)
        assertTrue(m.accountId != m.destinationAccountId)
    }
}
