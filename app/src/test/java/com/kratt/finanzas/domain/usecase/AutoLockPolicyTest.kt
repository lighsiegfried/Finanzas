package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.LockTimeout
import com.kratt.finanzas.domain.model.SecurityPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLockPolicyTest {

    @Test
    fun defaultTimeout_isSession() {
        assertEquals(LockTimeout.SESSION, SecurityPreferences.DEFAULT.lockTimeout)
    }

    @Test
    fun tenMinutesDuration_isSixHundredThousandMillis() {
        assertEquals(600_000L, LockTimeout.TEN_MINUTES.durationMillis)
    }

    @Test
    fun session_neverLocksByTime() {
        assertFalse(AutoLockPolicy.shouldLock(0L, 0L, LockTimeout.SESSION))
        assertFalse(AutoLockPolicy.shouldLock(0L, Long.MAX_VALUE / 2, LockTimeout.SESSION))
    }

    @Test
    fun session_neverLocks_evenWithClockAnomaly() {
        // aun con un delta negativo la sesion no se bloquea por tiempo
        assertFalse(AutoLockPolicy.shouldLock(10_000L, 5_000L, LockTimeout.SESSION))
    }

    @Test
    fun tenMinutes_staysUnlockedJustBeforeLimit() {
        assertFalse(AutoLockPolicy.shouldLock(0L, 599_999L, LockTimeout.TEN_MINUTES))
    }

    @Test
    fun tenMinutes_locksExactlyAtLimit() {
        assertTrue(AutoLockPolicy.shouldLock(0L, 600_000L, LockTimeout.TEN_MINUTES))
    }

    @Test
    fun tenMinutes_locksAfterLimit() {
        assertTrue(AutoLockPolicy.shouldLock(0L, 700_000L, LockTimeout.TEN_MINUTES))
    }

    @Test
    fun tenMinutes_negativeElapsed_locksAsSafetyMeasure() {
        assertTrue(AutoLockPolicy.shouldLock(10_000L, 5_000L, LockTimeout.TEN_MINUTES))
    }

    @Test
    fun tenMinutes_decisionDependsOnlyOnElapsedDelta() {
        val a = AutoLockPolicy.shouldLock(0L, 600_000L, LockTimeout.TEN_MINUTES)
        val b = AutoLockPolicy.shouldLock(1_000_000L, 1_600_000L, LockTimeout.TEN_MINUTES)
        assertEquals(a, b)
        assertTrue(a)
    }
}
