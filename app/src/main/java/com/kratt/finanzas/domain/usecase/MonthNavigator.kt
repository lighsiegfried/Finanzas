package com.kratt.finanzas.domain.usecase

import java.time.YearMonth

// navegacion determinista entre meses, usa yearmonth y no texto
object MonthNavigator {

    fun previous(month: YearMonth): YearMonth = month.minusMonths(1)

    fun next(month: YearMonth): YearMonth = month.plusMonths(1)

    fun isCurrent(month: YearMonth, today: YearMonth): Boolean = month == today
}
