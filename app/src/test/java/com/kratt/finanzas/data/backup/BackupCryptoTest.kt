package com.kratt.finanzas.data.backup

import javax.crypto.AEADBadTagException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoTest {

    private val key = ByteArray(32) { it.toByte() }
    private val iv = ByteArray(12) { (it + 1).toByte() }
    private val aad = "header".toByteArray()
    private val plaintext = "datos financieros".toByteArray()

    @Test
    fun roundTripReturnsPlaintext() {
        val ciphertext = BackupCrypto.encrypt(key, iv, aad, plaintext)
        val decrypted = BackupCrypto.decrypt(key, iv, aad, ciphertext)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun ciphertextCarriesGcmTag() {
        val ciphertext = BackupCrypto.encrypt(key, iv, aad, plaintext)
        assertEquals(plaintext.size + 16, ciphertext.size)
    }

    @Test
    fun tamperedCiphertextFails() {
        val ciphertext = BackupCrypto.encrypt(key, iv, aad, plaintext)
        ciphertext[0] = (ciphertext[0].toInt() xor 0x01).toByte()
        assertThrows(AEADBadTagException::class.java) {
            BackupCrypto.decrypt(key, iv, aad, ciphertext)
        }
    }

    @Test
    fun wrongKeyFails() {
        val ciphertext = BackupCrypto.encrypt(key, iv, aad, plaintext)
        val otherKey = ByteArray(32) { (it + 9).toByte() }
        assertThrows(AEADBadTagException::class.java) {
            BackupCrypto.decrypt(otherKey, iv, aad, ciphertext)
        }
    }

    @Test
    fun wrongAadFails() {
        val ciphertext = BackupCrypto.encrypt(key, iv, aad, plaintext)
        assertThrows(AEADBadTagException::class.java) {
            BackupCrypto.decrypt(key, iv, "otro".toByteArray(), ciphertext)
        }
    }

    @Test
    fun wrongIvFails() {
        val ciphertext = BackupCrypto.encrypt(key, iv, aad, plaintext)
        val otherIv = ByteArray(12) { 9 }
        assertThrows(AEADBadTagException::class.java) {
            BackupCrypto.decrypt(key, otherIv, aad, ciphertext)
        }
    }
}
