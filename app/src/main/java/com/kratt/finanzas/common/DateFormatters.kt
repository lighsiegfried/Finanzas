package com.kratt.finanzas.common

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// formatos de fecha en espanol de guatemala, siempre deterministas

object MonthFormatter {
    private val locale = Locale.forLanguageTag("es-GT")
    private val formatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", locale)

    // devuelve el mes como titulo, por ejemplo julio de 2026 con mayuscula inicial
    fun format(month: YearMonth): String =
        formatter.format(month).replaceFirstChar { it.titlecase(locale) }
}

object ShortDateFormatter {
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun format(date: LocalDate): String = formatter.format(date)
}
