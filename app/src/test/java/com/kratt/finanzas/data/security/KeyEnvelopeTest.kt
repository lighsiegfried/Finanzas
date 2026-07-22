package com.kratt.finanzas.data.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyEnvelopeTest {

    private fun sample() = KeyEnvelope(
        iv = ByteArray(12) { it.toByte() },
        ciphertext = ByteArray(48) { (it + 100).toByte() },
    )

    @Test
    fun encodeDecode_roundTrip_preservesIvAndCiphertext() {
        val original = sample()
        val decoded = KeyEnvelope.decode(original.encode())
        assertArrayEquals(original.iv, decoded.iv)
        assertArrayEquals(original.ciphertext, decoded.ciphertext)
    }

    @Test
    fun formatVersion_isOne() {
        assertEquals(1, KeyEnvelope.VERSION)
        // el byte de version viaja en la posicion cuatro del sobre
        assertEquals(1, sample().encode()[4].toInt())
    }

    @Test
    fun badMagic_isRejected() {
        val bytes = sample().encode()
        bytes[0] = 0x00
        assertThrows(KeyEnvelopeException.BadMagic::class.java) { KeyEnvelope.decode(bytes) }
    }

    @Test
    fun unknownVersion_isRejected() {
        val bytes = sample().encode()
        bytes[4] = 9
        assertThrows(KeyEnvelopeException.UnsupportedVersion::class.java) { KeyEnvelope.decode(bytes) }
    }

    @Test
    fun truncatedEnvelope_isRejected() {
        val bytes = sample().encode().copyOf(20)
        assertThrows(KeyEnvelopeException.Truncated::class.java) { KeyEnvelope.decode(bytes) }
    }

    @Test
    fun tooShortEnvelope_isRejected() {
        assertThrows(KeyEnvelopeException.BadLength::class.java) { KeyEnvelope.decode(ByteArray(3)) }
    }

    @Test
    fun tooLongEnvelope_isRejected() {
        assertThrows(KeyEnvelopeException.BadLength::class.java) { KeyEnvelope.decode(ByteArray(1000)) }
    }

    @Test
    fun ivLengthBelowMinimum_isRejected() {
        val bytes = sample().encode()
        bytes[5] = 5
        assertThrows(KeyEnvelopeException.BadLength::class.java) { KeyEnvelope.decode(bytes) }
    }
}
