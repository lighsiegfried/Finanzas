package com.kratt.finanzas.data.reminder

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// ajustes de recordatorios, datos no sensibles, nunca montos ni cuentas
data class ReminderSettings(
    val enabled: Boolean,
    val daysBefore: Int,
    val hour: Int = 9,
    val minute: Int = 0,
) {
    companion object {
        val DEFAULT = ReminderSettings(enabled = false, daysBefore = 1, hour = 9, minute = 0)
    }
}

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_preferences")

class ReminderPreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.reminderDataStore

    val settings: Flow<ReminderSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            ReminderSettings(
                enabled = prefs[ENABLED] ?: false,
                daysBefore = prefs[DAYS_BEFORE] ?: 1,
                hour = (prefs[HOUR] ?: 9).coerceIn(0, 23),
                minute = (prefs[MINUTE] ?: 0).coerceIn(0, 59),
            )
        }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[ENABLED] = enabled }
    }

    suspend fun setDaysBefore(days: Int) {
        dataStore.edit { it[DAYS_BEFORE] = days }
    }

    // guarda la hora preferida validando el rango, nunca datos sensibles
    suspend fun setTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[HOUR] = hour.coerceIn(0, 23)
            it[MINUTE] = minute.coerceIn(0, 59)
        }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("reminders_enabled")
        val DAYS_BEFORE = intPreferencesKey("reminder_days_before")
        val HOUR = intPreferencesKey("reminder_hour")
        val MINUTE = intPreferencesKey("reminder_minute")
    }
}
