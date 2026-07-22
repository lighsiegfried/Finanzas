package com.kratt.finanzas.security

import com.kratt.finanzas.domain.model.LockTimeout
import com.kratt.finanzas.domain.model.SecurityPreferences
import com.kratt.finanzas.domain.repository.SecurityPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockManagerTest {

    // repositorio falso en memoria para controlar las preferencias
    private class FakeSecurityPreferencesRepository(
        initial: SecurityPreferences,
    ) : SecurityPreferencesRepository {
        val state = MutableStateFlow(initial)
        override val preferences: Flow<SecurityPreferences> = state
        override suspend fun setAppLockEnabled(enabled: Boolean) {
            state.value = state.value.copy(appLockEnabled = enabled)
        }

        override suspend fun setLockTimeout(timeout: LockTimeout) {
            state.value = state.value.copy(lockTimeout = timeout)
        }
    }

    private var nowElapsed = 0L

    private fun TestScope.createManager(
        prefs: SecurityPreferences,
    ): Pair<AppLockManager, FakeSecurityPreferencesRepository> {
        val repository = FakeSecurityPreferencesRepository(prefs)
        val manager = AppLockManager(
            securityPreferencesRepository = repository,
            scope = backgroundScope,
            elapsedRealtime = { nowElapsed },
        )
        runCurrent()
        return manager to repository
    }

    private fun enabled(timeout: LockTimeout) =
        SecurityPreferences(appLockEnabled = true, lockTimeout = timeout)

    @Test
    fun startsUnlocked_whenLockDisabled() = runTest {
        val (manager, _) = createManager(SecurityPreferences.DEFAULT)
        assertEquals(LockSessionState.UNLOCKED, manager.sessionState.value)
    }

    @Test
    fun session_startsLocked_afterProcessRecreation() = runTest {
        // proceso nuevo con bloqueo activo arranca bloqueado
        val (manager, _) = createManager(enabled(LockTimeout.SESSION))
        assertEquals(LockSessionState.LOCKED, manager.sessionState.value)
    }

    @Test
    fun session_staysUnlocked_acrossBackgroundForegroundCycle() = runTest {
        val (manager, _) = createManager(enabled(LockTimeout.SESSION))
        manager.unlock()
        nowElapsed = 0L
        manager.onAppBackgrounded()
        // aunque pase mucho tiempo fuera, por sesion no se bloquea
        advanceTimeBy(3_600_000)
        runCurrent()
        nowElapsed = 3_600_000L
        manager.onAppForegrounded()
        assertEquals(LockSessionState.UNLOCKED, manager.sessionState.value)
    }

    @Test
    fun unlock_transitionsToUnlocked() = runTest {
        val (manager, _) = createManager(enabled(LockTimeout.SESSION))
        manager.unlock()
        assertEquals(LockSessionState.UNLOCKED, manager.sessionState.value)
    }

    @Test
    fun tenMinutes_remainsUnlockedBeforeLimit() = runTest {
        val (manager, _) = createManager(enabled(LockTimeout.TEN_MINUTES))
        manager.unlock()
        manager.onAppBackgrounded()
        advanceTimeBy(599_999)
        runCurrent()
        assertEquals(LockSessionState.UNLOCKED, manager.sessionState.value)
    }

    @Test
    fun tenMinutes_locksExactlyAtLimitInBackground() = runTest {
        val (manager, _) = createManager(enabled(LockTimeout.TEN_MINUTES))
        manager.unlock()
        manager.onAppBackgrounded()
        advanceTimeBy(600_000)
        runCurrent()
        assertEquals(LockSessionState.LOCKED, manager.sessionState.value)
    }

    @Test
    fun tenMinutes_locksAfterLimitOnForeground_usingMonotonicTime() = runTest {
        val (manager, _) = createManager(enabled(LockTimeout.TEN_MINUTES))
        manager.unlock()
        nowElapsed = 0L
        manager.onAppBackgrounded()
        // el delay virtual no corrio pero el tiempo monotonico real avanzo mas de 10 min
        nowElapsed = 600_001L
        manager.onAppForegrounded()
        assertEquals(LockSessionState.LOCKED, manager.sessionState.value)
    }

    @Test
    fun tenMinutes_foregroundBeforeLimit_cancelsPendingLock() = runTest {
        val (manager, _) = createManager(enabled(LockTimeout.TEN_MINUTES))
        manager.unlock()
        nowElapsed = 0L
        manager.onAppBackgrounded()
        advanceTimeBy(10_000)
        runCurrent()
        nowElapsed = 10_000L
        manager.onAppForegrounded()
        assertEquals(LockSessionState.UNLOCKED, manager.sessionState.value)
        advanceTimeBy(600_000)
        runCurrent()
        assertEquals(LockSessionState.UNLOCKED, manager.sessionState.value)
    }

    @Test
    fun disablingLock_unlocksSession() = runTest {
        val (manager, repository) = createManager(enabled(LockTimeout.SESSION))
        assertEquals(LockSessionState.LOCKED, manager.sessionState.value)
        repository.setAppLockEnabled(false)
        runCurrent()
        assertEquals(LockSessionState.UNLOCKED, manager.sessionState.value)
    }

    @Test
    fun lockNow_isIgnoredWhenLockDisabled() = runTest {
        val (manager, _) = createManager(SecurityPreferences.DEFAULT)
        manager.lockNow()
        assertEquals(LockSessionState.UNLOCKED, manager.sessionState.value)
    }
}
