package com.kratt.finanzas.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// recordatorios opcionales por meta y por compra; guarda solo ids, nunca montos ni datos sensibles
private val Context.planningReminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "planning_reminder_preferences")

class PlanningReminderPreferences(context: Context) {

    private val dataStore = context.applicationContext.planningReminderDataStore

    fun goalEnabled(goalId: Long): Flow<Boolean> = read(GOAL_IDS).map { it.contains(goalId.toString()) }
    fun purchaseEnabled(purchaseId: Long): Flow<Boolean> = read(PURCHASE_IDS).map { it.contains(purchaseId.toString()) }

    suspend fun enabledGoalIds(): Set<Long> = read(GOAL_IDS).first().mapNotNull { it.toLongOrNull() }.toSet()
    suspend fun enabledPurchaseIds(): Set<Long> = read(PURCHASE_IDS).first().mapNotNull { it.toLongOrNull() }.toSet()

    // dice si hay algun recordatorio activo, para decidir si vale la pena agendar el trabajo
    suspend fun hasAny(): Boolean = read(GOAL_IDS).first().isNotEmpty() || read(PURCHASE_IDS).first().isNotEmpty()

    suspend fun setGoalEnabled(goalId: Long, enabled: Boolean) = toggle(GOAL_IDS, goalId.toString(), enabled)
    suspend fun setPurchaseEnabled(purchaseId: Long, enabled: Boolean) = toggle(PURCHASE_IDS, purchaseId.toString(), enabled)

    private fun read(key: Preferences.Key<Set<String>>): Flow<Set<String>> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> prefs[key] ?: emptySet() }

    private suspend fun toggle(key: Preferences.Key<Set<String>>, id: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = if (enabled) current + id else current - id
        }
    }

    private companion object {
        val GOAL_IDS = stringSetPreferencesKey("reminder_goal_ids")
        val PURCHASE_IDS = stringSetPreferencesKey("reminder_purchase_ids")
    }
}
