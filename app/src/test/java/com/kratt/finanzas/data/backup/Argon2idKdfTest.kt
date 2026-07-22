package com.kratt.finanzas.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Argon2idKdfTest {

    // parametros ligeros para que la prueba corra rapido pero siga siendo argon2id real
    private fun lightParams(salt: ByteArray) = KdfParams(
        memoryKiB = 8,
        iterations = 1,
        parallelism = 1,
        outputLength = KdfParams.OUTPUT_LENGTH,
        salt = salt,
    )

    @Test
    fun deterministicForSameInput() {
        val salt = ByteArray(16) { it.toByte() }
        val a = Argon2idKdf.derive("password".toByteArray(), lightParams(salt))
        val b = Argon2idKdf.derive("password".toByteArray(), lightParams(salt))
        assertArrayEqualsResult(a, b)
        assertEquals(32, a.size)
    }

    @Test
    fun differentSaltChangesKey() {
        val a = Argon2idKdf.derive("password".toByteArray(), lightParams(ByteArray(16) { 1 }))
        val b = Argon2idKdf.derive("password".toByteArray(), lightParams(ByteArray(16) { 2 }))
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun differentPasswordChangesKey() {
        val salt = ByteArray(16) { 7 }
        val a = Argon2idKdf.derive("password-a".toByteArray(), lightParams(salt))
        val b = Argon2idKdf.derive("password-b".toByteArray(), lightParams(salt))
        assertFalse(a.contentEquals(b))
    }

    private fun assertArrayEqualsResult(a: ByteArray, b: ByteArray) {
        assertTrue("keys deben coincidir para misma entrada", a.contentEquals(b))
    }
}
