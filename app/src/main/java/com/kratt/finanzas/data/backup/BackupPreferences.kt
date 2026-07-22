package com.kratt.finanzas.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// metadatos no sensibles del ultimo respaldo, nunca guarda contrasenas ni claves
data class BackupMetadata(
    val lastBackupMillis: Long?,
    val formatVersion: Int?,
    val hasBackup: Boolean,
) {
    companion object {
        val EMPTY = BackupMetadata(null, null, false)
    }
}

private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "backup_preferences",
)

class BackupPreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.backupDataStore

    val metadata: Flow<BackupMetadata> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            val hasBackup = prefs[HAS_BACKUP] ?: false
            BackupMetadata(
                lastBackupMillis = if (hasBackup) prefs[LAST_MILLIS] else null,
                formatVersion = if (hasBackup) prefs[FORMAT_VERSION] else null,
                hasBackup = hasBackup,
            )
        }

    // guarda solo la marca de tiempo y la version, nada del contenido del respaldo
    suspend fun recordSuccessfulBackup(millis: Long, formatVersion: Int) {
        dataStore.edit { prefs ->
            prefs[LAST_MILLIS] = millis
            prefs[FORMAT_VERSION] = formatVersion
            prefs[HAS_BACKUP] = true
        }
    }

    private companion object {
        val LAST_MILLIS = longPreferencesKey("last_backup_millis")
        val FORMAT_VERSION = intPreferencesKey("backup_format_version")
        val HAS_BACKUP = booleanPreferencesKey("has_backup")
    }
}
