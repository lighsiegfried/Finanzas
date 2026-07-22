package com.kratt.finanzas.domain.usecase

import java.time.YearMonth

// rango de dias del mes en formato epoch day, listo para consultar la base
data class MonthRange(
    val startEpochDay: Long,
    val endEpochDay: Long,
) {
    companion object {
        fun of(month: YearMonth): MonthRange = MonthRange(
            startEpochDay = month.atDay(1).toEpochDay(),
            endEpochDay = month.atEndOfMonth().toEpochDay(),
        )
    }
}
