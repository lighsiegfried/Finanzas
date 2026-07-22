package com.kratt.finanzas.reminder

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

// calcula, sin depender de android, cuanto falta para la proxima hora del recordatorio
object ReminderScheduleCalculator {

    const val DEFAULT_HOUR = 9
    const val DEFAULT_MINUTE = 0

    // milisegundos desde ahora hasta la proxima vez que sean hora:minuto en la zona local
    fun initialDelayMillis(nowMillis: Long, zone: ZoneId, hour: Int, minute: Int): Long {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        var next = now.withHour(safeHour).withMinute(safeMinute).withSecond(0).withNano(0)
        // si la hora de hoy ya paso, se agenda para el dia siguiente
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }
}
