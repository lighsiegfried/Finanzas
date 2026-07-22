package com.kratt.finanzas.data.update

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

private val Context.updateContinuityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "update_continuity_preferences",
)

// preferencias no sensibles de continuidad; nunca guarda montos, saldos ni claves
class UpdateContinuityPreferences(context: Context) {

    private val dataStore = context.applicationContext.updateContinuityDataStore

    // ultima version que paso la verificacion de salud tras actualizar; 0 si aun no hay registro
    val lastSuccessfulVersionCode: Flow<Int> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[LAST_VERSION] ?: 0 }

    // si ya se mostro el aviso de crear respaldo antes de desinstalar
    val uninstallWarningShown: Flow<Boolean> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[UNINSTALL_WARNING_SHOWN] ?: false }

    suspend fun recordSuccessfulVersion(versionCode: Int) {
        dataStore.edit { it[LAST_VERSION] = versionCode }
    }

    suspend fun setUninstallWarningShown() {
        dataStore.edit { it[UNINSTALL_WARNING_SHOWN] = true }
    }

    private companion object {
        val LAST_VERSION = intPreferencesKey("last_successful_version_code")
        val UNINSTALL_WARNING_SHOWN = booleanPreferencesKey("uninstall_warning_shown")
    }
}
