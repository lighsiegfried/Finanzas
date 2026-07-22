package com.kratt.finanzas.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.FinanzasApplication
import com.kratt.finanzas.domain.model.LockTimeout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// persistencia real de las preferencias de seguridad en datastore
@RunWith(AndroidJUnit4::class)
class SecurityPreferencesDataStoreTest {

    private val repository
        get() = (ApplicationProvider.getApplicationContext<Context>()
            .applicationContext as FinanzasApplication).container.securityPreferencesRepository

    @Test
    fun updates_arePersistedAndReadBack() = runBlocking {
        repository.setAppLockEnabled(true)
        repository.setLockTimeout(LockTimeout.TEN_MINUTES)

        val prefs = repository.preferences.first()
        assertTrue(prefs.appLockEnabled)
        assertEquals(LockTimeout.TEN_MINUTES, prefs.lockTimeout)

        // regresa a los valores por defecto para no afectar otras pruebas
        repository.setAppLockEnabled(false)
        repository.setLockTimeout(LockTimeout.SESSION)
        val restored = repository.preferences.first()
        assertFalse(restored.appLockEnabled)
        assertEquals(LockTimeout.SESSION, restored.lockTimeout)
    }

    @Test
    fun flow_observesSequentialUpdates() = runBlocking {
        repository.setAppLockEnabled(false)
        assertFalse(repository.preferences.first().appLockEnabled)

        repository.setAppLockEnabled(true)
        assertTrue(repository.preferences.first().appLockEnabled)

        repository.setAppLockEnabled(false)
        assertFalse(repository.preferences.first().appLockEnabled)
    }
}
