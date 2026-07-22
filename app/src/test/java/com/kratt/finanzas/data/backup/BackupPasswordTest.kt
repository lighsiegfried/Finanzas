package com.kratt.finanzas.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPasswordTest {

    @Test
    fun emptyDetected() {
        assertTrue(BackupPassword.isEmpty(""))
        assertFalse(BackupPassword.isEmpty("a"))
    }

    @Test
    fun tooShortBelowTwelve() {
        assertTrue(BackupPassword.isTooShort("abcdefghijk")) // 11
        assertFalse(BackupPassword.isTooShort("abcdefghijkl")) // 12
    }

    @Test
    fun tooLongAbove128CodePoints() {
        val exactly128 = "a".repeat(128)
        val over = "a".repeat(129)
        assertFalse(BackupPassword.isTooLong(exactly128))
        assertTrue(BackupPassword.isTooLong(over))
    }

    @Test
    fun codePointsCountSurrogatePairsAsOne() {
        // dos emojis: length 4 en utf-16 pero 2 code points
        assertEquals(2, BackupPassword.codePoints("😀😀"))
    }

    @Test
    fun emojiPasswordNotTooShortByCodePoints() {
        val twelveEmoji = "😀".repeat(12)
        assertFalse(BackupPassword.isTooShort(twelveEmoji))
    }

    @Test
    fun matchesIsExactWithoutTrimming() {
        assertTrue(BackupPassword.matches("abc def ", "abc def "))
        assertFalse(BackupPassword.matches("abc", "abc "))
        assertFalse(BackupPassword.matches(" abc", "abc"))
    }
}
