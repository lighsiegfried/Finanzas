package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.RecurrenceType
import java.time.LocalDate

// calcula las fechas de un movimiento recurrente ancladas a la fecha de inicio
object RecurrenceScheduler {

    // avanza un numero de pasos desde la fecha de inicio, plus* recorta el fin de mes
    fun advance(startDate: LocalDate, type: RecurrenceType, interval: Int, steps: Int): LocalDate {
        val amount = interval.toLong() * steps
        return when (type) {
            RecurrenceType.WEEKLY -> startDate.plusWeeks(amount)
            RecurrenceType.MONTHLY -> startDate.plusMonths(amount)
            RecurrenceType.YEARLY -> startDate.plusYears(amount)
        }
    }

    // fechas programadas desde el inicio hasta el horizonte, respetando fin y un tope de ventana
    fun scheduledDates(
        startDate: LocalDate,
        type: RecurrenceType,
        interval: Int,
        endDate: LocalDate?,
        horizon: LocalDate,
        cap: Int = 60,
    ): List<LocalDate> {
        val dates = ArrayList<LocalDate>()
        var step = 0
        while (dates.size < cap) {
            val date = advance(startDate, type, interval, step)
            if (date.isAfter(horizon)) break
            if (endDate != null && date.isAfter(endDate)) break
            dates.add(date)
            step++
        }
        return dates
    }
}
