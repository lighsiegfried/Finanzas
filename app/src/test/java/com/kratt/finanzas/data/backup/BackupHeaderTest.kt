package com.kratt.finanzas.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupHeaderTest {

    private fun sampleKdf() = KdfParams(
        memoryKiB = 8,
        iterations = 1,
        parallelism = 1,
        outputLength = KdfParams.OUTPUT_LENGTH,
        salt = ByteArray(16) { it.toByte() },
    )

    private fun sampleHeader(payload: Long = 100L) =
        BackupHeader(kdf = sampleKdf(), iv = ByteArray(12) { (it + 3).toByte() }, payloadLength = payload)

    @Test
    fun encodedSizeIsFixed() {
        assertEquals(57, BackupHeader.SIZE)
        assertEquals(BackupHeader.SIZE, sampleHeader().encode().size)
    }

    @Test
    fun roundTripPreservesFields() {
        val header = sampleHeader(payload = 4096L)
        val decoded = BackupHeader.decode(header.encode())
        assertEquals(header.kdf.memoryKiB, decoded.kdf.memoryKiB)
        assertEquals(header.kdf.iterations, decoded.kdf.iterations)
        assertEquals(header.kdf.parallelism, decoded.kdf.parallelism)
        assertEquals(header.kdf.outputLength, decoded.kdf.outputLength)
        assertArrayEquals(header.kdf.salt, decoded.kdf.salt)
        assertArrayEquals(header.iv, decoded.iv)
        assertEquals(header.payloadLength, decoded.payloadLength)
        assertEquals(BackupHeader.FORMAT_VERSION, decoded.formatVersion)
    }

    @Test
    fun wrongSizeRejected() {
        assertThrows(BackupFormatException.InvalidLength::class.java) {
            BackupHeader.decode(ByteArray(10))
        }
    }

    @Test
    fun badMagicRejected() {
        val bytes = sampleHeader().encode()
        bytes[0] = 0x00
        assertThrows(BackupFormatException.BadMagic::class.java) {
            BackupHeader.decode(bytes)
        }
    }

    @Test
    fun unsupportedVersionRejected() {
        val bytes = sampleHeader().encode()
        bytes[4] = 2 // byte de version de formato
        assertThrows(BackupFormatException.UnsupportedVersion::class.java) {
            BackupHeader.decode(bytes)
        }
    }

    @Test
    fun unsupportedEncryptionRejected() {
        val bytes = sampleHeader().encode()
        bytes[35] = 2 // byte de id de cifrado
        assertThrows(BackupFormatException.UnsupportedEncryption::class.java) {
            BackupHeader.decode(bytes)
        }
    }

    @Test
    fun zeroPayloadRejected() {
        assertThrows(BackupFormatException.InvalidLength::class.java) {
            BackupHeader.decode(sampleHeader(payload = 0L).encode())
        }
    }
}
