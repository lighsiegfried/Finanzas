package com.kratt.finanzas.data.attachment

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class AttachmentCipherTest {

    private val random = SecureRandom()
    private fun key(): ByteArray = ByteArray(32).also { random.nextBytes(it) }

    @Test
    fun roundTripRecoversPlaintext() {
        val key = key()
        val plaintext = ByteArray(5000).also { random.nextBytes(it) }
        val encoded = AttachmentCipher.encode(key, plaintext, random)
        val decoded = AttachmentCipher.decode(key, encoded)
        assertArrayEquals(plaintext, decoded)
    }

    @Test
    fun encodedStartsWithMagicAndDiffersFromPlaintext() {
        val key = key()
        val plaintext = "recibo-tienda".toByteArray()
        val encoded = AttachmentCipher.encode(key, plaintext, random)
        assertArrayEquals(AttachmentCipher.MAGIC, encoded.copyOfRange(0, 4))
        assertFalse(encoded.copyOfRange(6, encoded.size).contentEquals(plaintext))
    }

    @Test
    fun twoEncodesUseDifferentIvs() {
        val key = key()
        val plaintext = "misma-entrada".toByteArray()
        val a = AttachmentCipher.encode(key, plaintext, random)
        val b = AttachmentCipher.encode(key, plaintext, random)
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun wrongKeyFailsAuthentication() {
        val plaintext = ByteArray(256).also { random.nextBytes(it) }
        val encoded = AttachmentCipher.encode(key(), plaintext, random)
        assertThrows(AttachmentCipherException::class.java) {
            AttachmentCipher.decode(key(), encoded)
        }
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        val key = key()
        val encoded = AttachmentCipher.encode(key, "importante".toByteArray(), random)
        encoded[encoded.size - 1] = (encoded[encoded.size - 1].toInt() xor 0x01).toByte()
        assertThrows(AttachmentCipherException::class.java) {
            AttachmentCipher.decode(key, encoded)
        }
    }

    @Test
    fun badMagicIsRejected() {
        val key = key()
        val encoded = AttachmentCipher.encode(key, "hola".toByteArray(), random)
        encoded[0] = 0x00
        assertThrows(AttachmentCipherException::class.java) {
            AttachmentCipher.decode(key, encoded)
        }
    }

    @Test
    fun truncatedInputIsRejected() {
        val key = key()
        assertThrows(AttachmentCipherException::class.java) {
            AttachmentCipher.decode(key, ByteArray(4))
        }
    }
}
