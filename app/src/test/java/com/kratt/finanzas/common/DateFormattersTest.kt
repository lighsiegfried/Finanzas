package com.kratt.finanzas.common

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormattersTest {

    @Test
    fun monthFormatter_formatsSpanishMonthCapitalized() {
        assertEquals("Julio de 2026", MonthFormatter.format(YearMonth.of(2026, 7)))
    }

    @Test
    fun monthFormatter_formatsJanuary() {
        assertEquals("Enero de 2027", MonthFormatter.format(YearMonth.of(2027, 1)))
    }

    @Test
    fun shortDateFormatter_usesDayMonthYear() {
        assertEquals("19/07/2026", ShortDateFormatter.format(LocalDate.of(2026, 7, 19)))
    }
}
