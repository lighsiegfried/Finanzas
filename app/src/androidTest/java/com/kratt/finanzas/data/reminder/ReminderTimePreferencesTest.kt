package com.kratt.finanzas.data.reminder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// verifica que la hora del recordatorio se guarda y se valida en datastore
@RunWith(AndroidJUnit4::class)
class ReminderTimePreferencesTest {

    private val repository = ReminderPreferencesRepository(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun setTime_roundTripsHourAndMinute() = runBlocking {
        repository.setTime(6, 30)
        val settings = repository.settings.first()
        assertEquals(6, settings.hour)
        assertEquals(30, settings.minute)
    }

    @Test
    fun setTime_outOfRange_isCoercedIntoValidRange() = runBlocking {
        repository.setTime(25, 70)
        val settings = repository.settings.first()
        assertEquals(23, settings.hour)
        assertEquals(59, settings.minute)
    }

    @Test
    fun setTime_negative_isCoercedToZero() = runBlocking {
        repository.setTime(-3, -1)
        val settings = repository.settings.first()
        assertEquals(0, settings.hour)
        assertEquals(0, settings.minute)
    }
}
