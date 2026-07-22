package com.kratt.finanzas.data.preferences

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import com.kratt.finanzas.domain.model.LockTimeout
import com.kratt.finanzas.domain.model.SecurityPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityPreferencesMappingTest {

    @Test
    fun emptyPreferences_produceSafeDefaults() {
        val prefs = emptyPreferences().toSecurityPreferences()
        assertEquals(SecurityPreferences.DEFAULT, prefs)
        assertFalse(prefs.appLockEnabled)
        assertEquals(LockTimeout.SESSION, prefs.lockTimeout)
    }

    @Test
    fun storedSession_mapsToSession() {
        assertEquals(LockTimeout.SESSION, migrateLockTimeout(LockTimeout.SESSION.name))
    }

    @Test
    fun storedTenMinutes_mapsToTenMinutes() {
        assertEquals(LockTimeout.TEN_MINUTES, migrateLockTimeout(LockTimeout.TEN_MINUTES.name))
    }

    @Test
    fun oldImmediate_mapsToSession() {
        assertEquals(LockTimeout.SESSION, migrateLockTimeout("IMMEDIATE"))
    }

    @Test
    fun oldThirtySeconds_mapsToSession() {
        assertEquals(LockTimeout.SESSION, migrateLockTimeout("AFTER_30_SECONDS"))
    }

    @Test
    fun oldOneMinute_mapsToSession() {
        assertEquals(LockTimeout.SESSION, migrateLockTimeout("AFTER_1_MINUTE"))
    }

    @Test
    fun oldFiveMinutes_mapsToTenMinutes() {
        assertEquals(LockTimeout.TEN_MINUTES, migrateLockTimeout("AFTER_5_MINUTES"))
    }

    @Test
    fun unknownOrNullValue_mapsToSession() {
        assertEquals(LockTimeout.SESSION, migrateLockTimeout("BOGUS_VALUE"))
        assertEquals(LockTimeout.SESSION, migrateLockTimeout(null))
    }

    @Test
    fun migration_preservesLockEnabledFlag() {
        // migrar el timeout viejo no debe reiniciar si el bloqueo esta activo
        val prefs = preferencesOf(
            SecurityPreferenceKeys.APP_LOCK_ENABLED to true,
            SecurityPreferenceKeys.LOCK_TIMEOUT to "AFTER_5_MINUTES",
        ).toSecurityPreferences()
        assertTrue(prefs.appLockEnabled)
        assertEquals(LockTimeout.TEN_MINUTES, prefs.lockTimeout)
    }
}
