package com.kratt.finanzas.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// preferencia no sensible de los widgets: si se permiten mostrar montos
private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_preferences")

class WidgetPreferences(context: Context) {

    private val dataStore = context.applicationContext.widgetDataStore

    // por defecto los montos estan ocultos en los widgets
    val showAmounts: Flow<Boolean> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> prefs[SHOW_AMOUNTS] ?: false }

    suspend fun showAmountsNow(): Boolean = showAmounts.first()

    suspend fun setShowAmounts(enabled: Boolean) = dataStore.edit { it[SHOW_AMOUNTS] = enabled }

    private companion object {
        val SHOW_AMOUNTS = booleanPreferencesKey("widget_show_amounts")
    }
}
