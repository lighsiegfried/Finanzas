package com.kratt.finanzas.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PortableBackupCodecTest {

    private fun manifest() = BackupManifest(
        roomSchemaVersion = 1,
        createdAtMillis = 1_700_000_000_000L,
        dbName = "finanzas.db",
        passphrase = ByteArray(32) { (it + 1).toByte() },
        encryptedDb = ByteArray(64) { it.toByte() },
    )

    private fun writeBackup(password: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        PortableBackupCodec.write(out, password, manifest())
        return out.toByteArray()
    }

    @Test
    fun writeThenReadRoundTrip() {
        val password = "correcthorsebattery".toByteArray()
        val bytes = writeBackup(password)
        val decoded = PortableBackupCodec.readManifest(ByteArrayInputStream(bytes), password)
        val original = manifest()
        assertEquals(original.dbName, decoded.dbName)
        assertEquals(original.createdAtMillis, decoded.createdAtMillis)
        assertArrayEquals(original.passphrase, decoded.passphrase)
        assertArrayEquals(original.encryptedDb, decoded.encryptedDb)
    }

    @Test
    fun wrongPasswordFailsAuthentication() {
        val bytes = writeBackup("password-correcta".toByteArray())
        assertThrows(BackupFormatException.AuthenticationFailed::class.java) {
            PortableBackupCodec.readManifest(ByteArrayInputStream(bytes), "password-incorrecta".toByteArray())
        }
    }

    @Test
    fun corruptedPayloadFailsAuthentication() {
        val password = "password-correcta".toByteArray()
        val bytes = writeBackup(password)
        bytes[BackupHeader.SIZE + 2] = (bytes[BackupHeader.SIZE + 2].toInt() xor 0x01).toByte()
        assertThrows(BackupFormatException.AuthenticationFailed::class.java) {
            PortableBackupCodec.readManifest(ByteArrayInputStream(bytes), password)
        }
    }

    @Test
    fun trailingDataRejected() {
        val password = "password-correcta".toByteArray()
        val bytes = writeBackup(password) + byteArrayOf(0x00)
        assertThrows(BackupFormatException.InvalidLength::class.java) {
            PortableBackupCodec.readManifest(ByteArrayInputStream(bytes), password)
        }
    }

    @Test
    fun truncatedHeaderRejected() {
        val bytes = writeBackup("password-correcta".toByteArray())
        assertThrows(BackupFormatException.Truncated::class.java) {
            PortableBackupCodec.readManifest(ByteArrayInputStream(bytes.copyOfRange(0, 30)), "password-correcta".toByteArray())
        }
    }

    @Test
    fun badMagicRejected() {
        val bytes = writeBackup("password-correcta".toByteArray())
        bytes[0] = 0x00
        assertThrows(BackupFormatException.BadMagic::class.java) {
            PortableBackupCodec.readManifest(ByteArrayInputStream(bytes), "password-correcta".toByteArray())
        }
    }
}
