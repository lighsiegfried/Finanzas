package com.kratt.finanzas.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateContinuityTest {

    @Test
    fun first_install_when_no_version_recorded() {
        assertEquals(UpdateSituation.FIRST_INSTALL, UpdateContinuity.situation(lastSuccessfulVersionCode = 0, currentVersionCode = 10000))
    }

    @Test
    fun updated_when_current_is_higher() {
        assertEquals(UpdateSituation.UPDATED, UpdateContinuity.situation(lastSuccessfulVersionCode = 10000, currentVersionCode = 10001))
    }

    @Test
    fun same_version_when_equal() {
        assertEquals(UpdateSituation.SAME_VERSION, UpdateContinuity.situation(lastSuccessfulVersionCode = 10001, currentVersionCode = 10001))
    }

    @Test
    fun downgrade_is_not_treated_as_update() {
        // versionCode nunca deberia bajar; si pasara no se anuncia una actualizacion
        assertEquals(UpdateSituation.SAME_VERSION, UpdateContinuity.situation(lastSuccessfulVersionCode = 10002, currentVersionCode = 10001))
    }

    private val day = 86_400_000L

    @Test
    fun backup_age_is_null_without_backup() {
        assertNull(BackupFreshness.ageDays(lastBackupMillis = null, nowMillis = 100 * day))
    }

    @Test
    fun backup_age_in_days() {
        assertEquals(5L, BackupFreshness.ageDays(lastBackupMillis = 100 * day, nowMillis = 105 * day))
    }

    @Test
    fun no_backup_is_not_stale() {
        assertFalse(BackupFreshness.isStale(lastBackupMillis = null, nowMillis = 100 * day))
    }

    @Test
    fun recent_backup_is_not_stale() {
        assertFalse(BackupFreshness.isStale(lastBackupMillis = 100 * day, nowMillis = 120 * day))
    }

    @Test
    fun old_backup_is_stale_after_thirty_days() {
        assertTrue(BackupFreshness.isStale(lastBackupMillis = 100 * day, nowMillis = 131 * day))
    }
}
