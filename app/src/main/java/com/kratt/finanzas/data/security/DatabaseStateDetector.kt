package com.kratt.finanzas.data.security

import java.io.File

class DatabaseStateDetector(
    private val dbFile: File,
    private val markerFile: File,
) {

    // revisa si el archivo empieza con la cabecera de un sqlite sin cifrar
    fun isPlaintext(file: File): Boolean {
        if (!file.exists() || file.length() < SQLITE_HEADER.size) return false
        val head = ByteArray(SQLITE_HEADER.size)
        file.inputStream().use { input ->
            var read = 0
            while (read < head.size) {
                val r = input.read(head, read, head.size - read)
                if (r < 0) return false
                read += r
            }
        }
        return head.contentEquals(SQLITE_HEADER)
    }

    // decide el estado sin borrar nada ni suponer que algo ilegible esta vacio
    fun detect(envelopeExists: Boolean, wrappingKeyExists: Boolean): DatabaseState {
        if (markerFile.exists()) return DatabaseState.MIGRATION_IN_PROGRESS
        if (!dbFile.exists()) return DatabaseState.NEW_INSTALL
        if (isPlaintext(dbFile)) return DatabaseState.PLAINTEXT_READY_FOR_MIGRATION
        // la base existe y no es texto plano, se asume cifrada
        if (!envelopeExists || !wrappingKeyExists) return DatabaseState.RECOVERY_REQUIRED
        return DatabaseState.ENCRYPTED_READY
    }

    companion object {
        // cabecera de un sqlite sin cifrar: "SQLite format 3" y un byte cero final
        private val SQLITE_HEADER = byteArrayOf(
            0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66,
            0x6F, 0x72, 0x6D, 0x61, 0x74, 0x20, 0x33, 0x00,
        )
    }
}
