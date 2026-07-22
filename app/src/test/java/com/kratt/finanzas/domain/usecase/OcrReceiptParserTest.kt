package com.kratt.finanzas.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OcrReceiptParserTest {

    private val receipt = """
        Super Tienda La Economia
        NIT: 1234567-8
        Fecha: 15/03/2026
        Factura No. A-00123
        Leche         Q 12.50
        Pan           Q  8.00
        Subtotal      Q 20.50
        Total         Q 25.75
        Gracias por su compra
    """.trimIndent()

    @Test
    fun parsesMerchantDateTotalAndInvoice() {
        val s = OcrReceiptParser.parse(receipt)
        assertEquals("Super Tienda La Economia", s.merchant)
        assertEquals(LocalDate.of(2026, 3, 15).toEpochDay(), s.dateEpochDay)
        assertEquals(2575L, s.totalCents)
        assertEquals("A-00123", s.invoiceNumber)
        assertTrue(s.hasAny)
    }

    @Test
    fun totalIgnoresSubtotal() {
        val text = "Subtotal Q 100.00\nTotal Q 112.00"
        assertEquals(11200L, OcrReceiptParser.parse(text).totalCents)
    }

    @Test
    fun emptyTextYieldsNothing() {
        val s = OcrReceiptParser.parse("")
        assertFalse(s.hasAny)
        assertNull(s.totalCents)
        assertNull(s.dateEpochDay)
    }

    @Test
    fun amountParsingHandlesGuatemalanAndEuropeanFormats() {
        assertEquals(123456L, OcrReceiptParser.parseAmountToCents("Q1,234.56"))
        assertEquals(123456L, OcrReceiptParser.parseAmountToCents("1234.56"))
        assertEquals(123456L, OcrReceiptParser.parseAmountToCents("1.234,56"))
        assertEquals(2050L, OcrReceiptParser.parseAmountToCents("20,50"))
        assertEquals(800L, OcrReceiptParser.parseAmountToCents("Q 8.00"))
    }

    @Test
    fun amountParsingRejectsInvalidAndNegative() {
        assertNull(OcrReceiptParser.parseAmountToCents("abc"))
        assertNull(OcrReceiptParser.parseAmountToCents("-5.00"))
        assertNull(OcrReceiptParser.parseAmountToCents(""))
    }
}
