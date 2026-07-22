package com.kratt.finanzas.reminder

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderScheduleCalculatorTest {

    private val guatemala = ZoneId.of("America/Guatemala")
    private val hour = 60L * 60L * 1000L

    private fun nowAt(h: Int, m: Int): Long =
        ZonedDateTime.of(2026, 7, 20, h, m, 0, 0, guatemala).toInstant().toEpochMilli()

    @Test
    fun defaultNineAm_fromEightAm_isOneHour() {
        val delay = ReminderScheduleCalculator.initialDelayMillis(nowAt(8, 0), guatemala, 9, 0)
        assertEquals(hour, delay)
    }

    @Test
    fun timeAlreadyPassedToday_schedulesForTomorrow() {
        // a las 10:00 la hora de las 09:00 ya paso, se agenda 23 horas despues
        val delay = ReminderScheduleCalculator.initialDelayMillis(nowAt(10, 0), guatemala, 9, 0)
        assertEquals(23 * hour, delay)
    }

    @Test
    fun midnightTarget_fromElevenPm_isOneHour() {
        val delay = ReminderScheduleCalculator.initialDelayMillis(nowAt(23, 0), guatemala, 0, 0)
        assertEquals(hour, delay)
    }

    @Test
    fun endOfDayTarget_fromElevenPm_isFiftyNineMinutes() {
        val delay = ReminderScheduleCalculator.initialDelayMillis(nowAt(23, 0), guatemala, 23, 59)
        assertEquals(59L * 60L * 1000L, delay)
    }

    @Test
    fun timezoneChangesTheDelay() {
        val instant = nowAt(8, 0) // 08:00 en guatemala equivale a 14:00 utc
        val localDelay = ReminderScheduleCalculator.initialDelayMillis(instant, guatemala, 9, 0)
        val utcDelay = ReminderScheduleCalculator.initialDelayMillis(instant, ZoneId.of("UTC"), 9, 0)
        assertEquals(hour, localDelay)
        assertEquals(19 * hour, utcDelay)
    }

    @Test
    fun delayIsAlwaysWithinOneDay() {
        val delay = ReminderScheduleCalculator.initialDelayMillis(nowAt(9, 0), guatemala, 9, 0)
        // exactamente a la hora se agenda para el dia siguiente, nunca cero ni negativo
        assertTrue(delay in 1..(24 * hour))
        assertEquals(24 * hour, delay)
    }
}
