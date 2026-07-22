package com.kratt.finanzas.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenProtectionTest {

    @Test
    fun releaseBuild_appliesSecureFlag() {
        assertTrue(ScreenProtection.shouldApplySecureFlag(isDebugBuild = false))
    }

    @Test
    fun debugBuild_allowsScreenshotsForEvidence() {
        assertFalse(ScreenProtection.shouldApplySecureFlag(isDebugBuild = true))
    }
}
