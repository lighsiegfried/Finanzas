package com.kratt.finanzas.data.backup

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupFileNameTest {

    @Test
    fun formatsWithDateAndTime() {
        val name = BackupFileName.generate(LocalDateTime.of(2026, 7, 19, 15, 30))
        assertEquals("mis-finanzas-2026-07-19-1530.mfinanzas", name)
    }

    @Test
    fun zeroPadsSingleDigitFields() {
        val name = BackupFileName.generate(LocalDateTime.of(2026, 1, 5, 9, 3))
        assertEquals("mis-finanzas-2026-01-05-0903.mfinanzas", name)
    }

    @Test
    fun extensionAndMimeAreStable() {
        assertEquals(".mfinanzas", BackupFileName.EXTENSION)
        assertEquals("application/octet-stream", BackupFileName.MIME_TYPE)
    }
}
