package com.kratt.finanzas.domain.usecase

import java.time.LocalDate
import java.time.YearMonth

// periodos deterministas para los reportes, siempre en fechas locales
enum class ReportPeriod { THIS_MONTH, LAST_MONTH, LAST_3_MONTHS, LAST_6_MONTHS, THIS_YEAR, CUSTOM }

data class DateRange(val start: LocalDate, val end: LocalDate) {
    val isValid: Boolean get() = !start.isAfter(end)
}

object ReportPeriods {

    // convierte un periodo en un rango de fechas usando el mes actual como ancla
    fun range(period: ReportPeriod, today: LocalDate, customStart: LocalDate? = null, customEnd: LocalDate? = null): DateRange {
        val month = YearMonth.from(today)
        return when (period) {
            ReportPeriod.THIS_MONTH -> DateRange(month.atDay(1), month.atEndOfMonth())
            ReportPeriod.LAST_MONTH -> month.minusMonths(1).let { DateRange(it.atDay(1), it.atEndOfMonth()) }
            ReportPeriod.LAST_3_MONTHS -> DateRange(month.minusMonths(2).atDay(1), month.atEndOfMonth())
            ReportPeriod.LAST_6_MONTHS -> DateRange(month.minusMonths(5).atDay(1), month.atEndOfMonth())
            ReportPeriod.THIS_YEAR -> DateRange(LocalDate.of(today.year, 1, 1), LocalDate.of(today.year, 12, 31))
            ReportPeriod.CUSTOM -> DateRange(customStart ?: month.atDay(1), customEnd ?: month.atEndOfMonth())
        }
    }
}
