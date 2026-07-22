package com.kratt.finanzas.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManifestTest {

    private fun sample(schema: Int = 1) = BackupManifest(
        roomSchemaVersion = schema,
        createdAtMillis = 1_700_000_000_000L,
        dbName = "finanzas.db",
        passphrase = ByteArray(32) { it.toByte() },
        encryptedDb = byteArrayOf(1, 2, 3, 4, 5),
    )

    @Test
    fun roundTripPreservesFields() {
        val manifest = sample()
        val decoded = BackupManifest.decode(manifest.encode())
        assertEquals(manifest.roomSchemaVersion, decoded.roomSchemaVersion)
        assertEquals(manifest.createdAtMillis, decoded.createdAtMillis)
        assertEquals(manifest.dbName, decoded.dbName)
        assertArrayEquals(manifest.passphrase, decoded.passphrase)
        assertArrayEquals(manifest.encryptedDb, decoded.encryptedDb)
    }

    @Test
    fun trailingDataRejected() {
        val encoded = sample().encode()
        val withExtra = encoded + byteArrayOf(0x00)
        assertThrows(BackupFormatException.InvalidLength::class.java) {
            BackupManifest.decode(withExtra)
        }
    }

    @Test
    fun truncatedRejected() {
        val encoded = sample().encode()
        assertThrows(BackupFormatException.Truncated::class.java) {
            BackupManifest.decode(encoded.copyOfRange(0, 3))
        }
    }

    @Test
    fun unsupportedSchemaRejected() {
        assertThrows(BackupFormatException.UnsupportedSchema::class.java) {
            BackupManifest.decode(sample(schema = 99).encode())
        }
    }

    @Test
    fun unsupportedManifestVersionRejected() {
        val encoded = sample().encode()
        encoded[0] = 9 // byte de version de manifiesto
        assertThrows(BackupFormatException.UnsupportedVersion::class.java) {
            BackupManifest.decode(encoded)
        }
    }

    @Test
    fun schema6IsAccepted() {
        val decoded = BackupManifest.decode(sample(schema = 6).encode())
        assertEquals(6, decoded.roomSchemaVersion)
    }

    @Test
    fun v2RoundTripPreservesAttachments() {
        val manifest = BackupManifest(
            roomSchemaVersion = 6,
            createdAtMillis = 1_700_000_000_000L,
            dbName = "finanzas.db",
            passphrase = ByteArray(32) { it.toByte() },
            encryptedDb = byteArrayOf(9, 8, 7),
            attachments = listOf(
                BackupAttachment("aaaa.enc", byteArrayOf(1, 2, 3, 4)),
                BackupAttachment("bbbb.enc", ByteArray(1000) { (it % 7).toByte() }),
            ),
        )
        val decoded = BackupManifest.decode(manifest.encode())
        assertEquals(2, decoded.manifestVersion)
        assertEquals(2, decoded.attachments.size)
        assertEquals("aaaa.enc", decoded.attachments[0].storedFileName)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), decoded.attachments[0].plaintext)
        assertEquals("bbbb.enc", decoded.attachments[1].storedFileName)
        assertEquals(1000, decoded.attachments[1].plaintext.size)
    }

    @Test
    fun v1BackupWithoutAttachmentsStillDecodes() {
        // un respaldo previo a 5D no lleva seccion de adjuntos
        val v1 = BackupManifest(
            roomSchemaVersion = 5,
            createdAtMillis = 1_700_000_000_000L,
            dbName = "finanzas.db",
            passphrase = ByteArray(32) { it.toByte() },
            encryptedDb = byteArrayOf(1, 2, 3),
            manifestVersion = 1,
        )
        val decoded = BackupManifest.decode(v1.encode())
        assertEquals(1, decoded.manifestVersion)
        assertEquals(5, decoded.roomSchemaVersion)
        assertTrue(decoded.attachments.isEmpty())
    }
}
