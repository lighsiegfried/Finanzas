package com.kratt.finanzas.presentation.theme

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicColorPolicyTest {

    @Test
    fun available_fromAndroid12() {
        assertTrue(DynamicColorPolicy.isAvailable(Build.VERSION_CODES.S))
        assertTrue(DynamicColorPolicy.isAvailable(36))
    }

    @Test
    fun notAvailable_belowAndroid12() {
        assertFalse(DynamicColorPolicy.isAvailable(Build.VERSION_CODES.R))
        assertFalse(DynamicColorPolicy.isAvailable(26))
    }
}
