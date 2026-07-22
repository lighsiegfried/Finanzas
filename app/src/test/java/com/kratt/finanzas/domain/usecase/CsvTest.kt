package com.kratt.finanzas.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {

    @Test
    fun preventsFormulaInjection() {
        assertEquals("'=1+2", Csv.textField("=1+2"))
        assertEquals("'+cmd", Csv.textField("+cmd"))
        assertEquals("'-cmd", Csv.textField("-cmd"))
        assertEquals("'@ref", Csv.textField("@ref"))
    }

    @Test
    fun escapesCommasQuotesAndNewlines() {
        assertEquals("\"a,b\"", Csv.textField("a,b"))
        assertEquals("\"a\"\"b\"", Csv.textField("a\"b"))
        assertEquals("\"line1\nline2\"", Csv.textField("line1\nline2"))
    }

    @Test
    fun formulaValueWithCommaIsPrefixedAndQuoted() {
        assertEquals("\"'=SUM(A1),x\"", Csv.textField("=SUM(A1),x"))
    }

    @Test
    fun numericFieldStaysMachineReadable() {
        // los numeros no llevan prefijo de formula, aunque un negativo empiece con guion
        assertEquals("1234.56", Csv.field("1234.56"))
        assertEquals("-99.00", Csv.field("-99.00"))
    }

    @Test
    fun rowJoinsWithCommas() {
        assertEquals("Alimentacion,850.00", Csv.row(listOf(Csv.textField("Alimentacion"), Csv.field("850.00"))))
    }
}
