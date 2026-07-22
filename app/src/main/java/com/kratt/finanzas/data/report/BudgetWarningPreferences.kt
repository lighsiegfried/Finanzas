package com.kratt.finanzas.data.report

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.budgetWarningDataStore: DataStore<Preferences> by preferencesDataStore(name = "budget_warnings")

// recuerda que avisos ya se mostraron, sin guardar montos ni datos financieros
// la clave incluye el mes, asi el estado se reinicia solo al cambiar de mes
class BudgetWarningPreferences(context: Context) {

    private val dataStore = context.applicationContext.budgetWarningDataStore

    suspend fun delivered(): Set<String> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[DELIVERED] ?: emptySet() }
        .first()

    suspend fun wasDelivered(key: String): Boolean = key in delivered()

    suspend fun markDelivered(key: String) {
        dataStore.edit { prefs ->
            prefs[DELIVERED] = (prefs[DELIVERED] ?: emptySet()) + key
        }
    }

    private companion object {
        val DELIVERED = stringSetPreferencesKey("delivered_thresholds")
    }
}
