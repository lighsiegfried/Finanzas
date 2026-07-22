package com.kratt.finanzas.data.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyStore
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// prueba el envoltorio de la frase con una clave de keystore de prueba
@RunWith(AndroidJUnit4::class)
class DatabaseKeyManagerTest {

    private val alias = "finanzas_test_wrapping_key"
    private val keyManager = DatabaseKeyManager(alias)

    @After
    fun cleanup() {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
    }

    private fun passphrase() = ByteArray(32) { (it + 1).toByte() }

    @Test
    fun wrapThenUnwrap_returnsSamePassphrase() {
        val secret = passphrase()
        val recovered = keyManager.unwrap(keyManager.wrap(secret))
        assertArrayEquals(secret, recovered)
        assertTrue(keyManager.hasWrappingKey())
    }

    @Test
    fun tamperedCiphertext_failsAuthentication() {
        val envelope = keyManager.wrap(passphrase())
        val tampered = KeyEnvelope(envelope.iv, envelope.ciphertext.copyOf().also { it[0] = (it[0] + 1).toByte() })
        assertThrows(KeyUnwrapException::class.java) { keyManager.unwrap(tampered) }
    }

    @Test
    fun tamperedIv_failsAuthentication() {
        val envelope = keyManager.wrap(passphrase())
        val tampered = KeyEnvelope(envelope.iv.copyOf().also { it[0] = (it[0] + 1).toByte() }, envelope.ciphertext)
        assertThrows(KeyUnwrapException::class.java) { keyManager.unwrap(tampered) }
    }

    @Test
    fun eachWrap_usesFreshIv() {
        val a = keyManager.wrap(passphrase())
        val b = keyManager.wrap(passphrase())
        // dos operaciones no deben reutilizar el mismo iv
        assertTrue(!a.iv.contentEquals(b.iv))
    }
}
