package com.kratt.finanzas.data.security

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DatabaseStateDetectorTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val plaintextHeader = byteArrayOf(
        0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66,
        0x6F, 0x72, 0x6D, 0x61, 0x74, 0x20, 0x33, 0x00,
    )

    private fun detector(db: File, marker: File) = DatabaseStateDetector(db, marker)

    private fun file(name: String) = File(tmp.root, name)

    @Test
    fun missingDatabase_isNewInstall() {
        val d = detector(file("db"), file("marker"))
        assertEquals(DatabaseState.NEW_INSTALL, d.detect(envelopeExists = false, wrappingKeyExists = false))
    }

    @Test
    fun plaintextHeader_isDetected_andReportsMigration() {
        val db = file("db").apply { writeBytes(plaintextHeader + ByteArray(100)) }
        val d = detector(db, file("marker"))
        assertTrue(d.isPlaintext(db))
        assertEquals(
            DatabaseState.PLAINTEXT_READY_FOR_MIGRATION,
            d.detect(envelopeExists = true, wrappingKeyExists = true),
        )
    }

    @Test
    fun encryptedFile_withEnvelopeAndKey_isReady() {
        // un archivo que no empieza con la cabecera se clasifica como cifrado
        val db = file("db").apply { writeBytes(ByteArray(200) { 0x7F }) }
        val d = detector(db, file("marker"))
        assertFalse(d.isPlaintext(db))
        assertEquals(
            DatabaseState.ENCRYPTED_READY,
            d.detect(envelopeExists = true, wrappingKeyExists = true),
        )
    }

    @Test
    fun encryptedFile_missingEnvelope_requiresRecovery() {
        val db = file("db").apply { writeBytes(ByteArray(200) { 0x7F }) }
        val d = detector(db, file("marker"))
        assertEquals(
            DatabaseState.RECOVERY_REQUIRED,
            d.detect(envelopeExists = false, wrappingKeyExists = true),
        )
    }

    @Test
    fun encryptedFile_missingWrappingKey_requiresRecovery() {
        val db = file("db").apply { writeBytes(ByteArray(200) { 0x7F }) }
        val d = detector(db, file("marker"))
        assertEquals(
            DatabaseState.RECOVERY_REQUIRED,
            d.detect(envelopeExists = true, wrappingKeyExists = false),
        )
    }

    @Test
    fun marker_reportsMigrationInProgress() {
        val db = file("db").apply { writeBytes(ByteArray(200) { 0x7F }) }
        val marker = file("marker").apply { writeBytes(byteArrayOf(1)) }
        val d = detector(db, marker)
        assertEquals(
            DatabaseState.MIGRATION_IN_PROGRESS,
            d.detect(envelopeExists = true, wrappingKeyExists = true),
        )
    }

    @Test
    fun unreadableShortFile_isNotTreatedAsPlaintextOrEmpty() {
        // un archivo corto e ilegible no es texto plano y no se asume vacio
        val db = file("db").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val d = detector(db, file("marker"))
        assertFalse(d.isPlaintext(db))
        assertEquals(
            DatabaseState.ENCRYPTED_READY,
            d.detect(envelopeExists = true, wrappingKeyExists = true),
        )
    }
}
