package com.kratt.finanzas.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AttachmentChecksumTest {

    @Test
    fun knownVectors() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            AttachmentChecksum.sha256Hex(ByteArray(0)),
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            AttachmentChecksum.sha256Hex("abc".toByteArray()),
        )
    }

    @Test
    fun differentContentDiffersAndIsLowercaseHex() {
        val a = AttachmentChecksum.sha256Hex("recibo-1".toByteArray())
        val b = AttachmentChecksum.sha256Hex("recibo-2".toByteArray())
        assertNotEquals(a, b)
        assertEquals(64, a.length)
        assertEquals(a, a.lowercase())
    }
}
