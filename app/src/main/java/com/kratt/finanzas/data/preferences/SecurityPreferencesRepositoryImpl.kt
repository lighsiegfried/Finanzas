package com.kratt.finanzas.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kratt.finanzas.domain.model.LockTimeout
import com.kratt.finanzas.domain.model.SecurityPreferences
import com.kratt.finanzas.domain.repository.SecurityPreferencesRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// unica instancia de datastore para las preferencias de seguridad
private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "security_preferences",
)

object SecurityPreferenceKeys {
    val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    val LOCK_TIMEOUT = stringPreferencesKey("lock_timeout")
}

// convierte las preferencias crudas al modelo de dominio con valores seguros
fun Preferences.toSecurityPreferences(): SecurityPreferences = SecurityPreferences(
    appLockEnabled = this[SecurityPreferenceKeys.APP_LOCK_ENABLED] ?: false,
    lockTimeout = migrateLockTimeout(this[SecurityPreferenceKeys.LOCK_TIMEOUT]),
)

// migra los valores viejos de timeout a las dos opciones nuevas
// cualquier valor desconocido o corrupto usa la opcion segura por sesion
internal fun migrateLockTimeout(stored: String?): LockTimeout = when (stored) {
    LockTimeout.TEN_MINUTES.name -> LockTimeout.TEN_MINUTES
    "AFTER_5_MINUTES", "FIVE_MINUTES" -> LockTimeout.TEN_MINUTES
    else -> LockTimeout.SESSION
}

class SecurityPreferencesRepositoryImpl(context: Context) : SecurityPreferencesRepository {

    private val dataStore = context.applicationContext.securityDataStore

    // si el archivo se corrompe se usan los valores por defecto en vez de fallar
    override val preferences: Flow<SecurityPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toSecurityPreferences() }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SecurityPreferenceKeys.APP_LOCK_ENABLED] = enabled }
    }

    override suspend fun setLockTimeout(timeout: LockTimeout) {
        dataStore.edit { prefs -> prefs[SecurityPreferenceKeys.LOCK_TIMEOUT] = timeout.name }
    }
}
