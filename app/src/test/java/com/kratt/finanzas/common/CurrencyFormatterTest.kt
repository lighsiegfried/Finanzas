package com.kratt.finanzas.common

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun format_zero_returnsZeroQuetzales() {
        assertEquals("Q0.00", CurrencyFormatter.format(0))
    }

    @Test
    fun format_examplePositive_matchesSpec() {
        assertEquals("Q125.75", CurrencyFormatter.format(12_575))
    }

    @Test
    fun format_exampleNegative_matchesSpec() {
        assertEquals("-Q125.75", CurrencyFormatter.format(-12_575))
    }

    @Test
    fun format_thousands_groupsWithComma() {
        assertEquals("Q1,234.56", CurrencyFormatter.format(123_456))
    }

    @Test
    fun format_subQuetzal_keepsTwoDecimals() {
        assertEquals("Q0.50", CurrencyFormatter.format(50))
    }

    @Test
    fun format_negative_prefixesMinusBeforeSymbol() {
        assertEquals("-Q5.00", CurrencyFormatter.format(-500))
    }

    @Test
    fun format_largeValue_groupsAllThousands() {
        assertEquals("Q12,345,678.90", CurrencyFormatter.format(1_234_567_890))
    }
}
