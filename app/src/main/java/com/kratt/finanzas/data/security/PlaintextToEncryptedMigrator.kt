package com.kratt.finanzas.data.security

import android.database.Cursor
import java.io.File
import android.database.sqlite.SQLiteDatabase as FrameworkDatabase
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase

// fallo de validacion de la migracion, el mensaje no lleva datos sensibles
class MigrationValidationException(message: String) : Exception(message)

// migra la base de texto plano a sqlcipher sin borrar el original hasta validar
class PlaintextToEncryptedMigrator(
    private val files: DatabaseFiles,
    // valida el candidato abriendolo con la configuracion real de room
    private val roomValidator: (File, ByteArray) -> Unit,
) {
    // ejecuta la migracion completa: exporta, valida y reemplaza de forma atomica
    fun migrate(passphrase: ByteArray) {
        SqlCipherNative.ensureLoaded()
        files.marker.writeBytes(byteArrayOf(1))
        // descarta cualquier candidato incompleto de un intento anterior
        files.candidate.delete()

        checkpointPlaintext()
        val before = readPlaintextStats()
        exportToEncrypted(passphrase)
        val after = readEncryptedStats(passphrase)
        if (before != after) throw MigrationValidationException("row or aggregate mismatch")

        roomValidator(files.candidate, passphrase)
        atomicReplace()
        files.marker.delete()
    }

    // fuerza el checkpoint del wal para que todo quede en el archivo principal
    private fun checkpointPlaintext() {
        val db = FrameworkDatabase.openDatabase(files.db.path, null, FrameworkDatabase.OPEN_READWRITE)
        try {
            db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
        } finally {
            db.close()
        }
    }

    // copia todo el contenido al cifrado usando sqlcipher_export
    private fun exportToEncrypted(passphrase: ByteArray) {
        val enc = CipherDatabase.openOrCreateDatabase(files.candidate, passphrase, null, null)
        try {
            enc.rawExecSQL("ATTACH DATABASE '${files.db.path}' AS plaintext KEY '';")
            enc.rawQuery("SELECT sqlcipher_export('main', 'plaintext')", null).use { it.moveToFirst() }
            // copia la version del esquema para que room no vuelva a sembrar la base
            val version = enc.rawQuery("PRAGMA plaintext.user_version", null)
                .use { if (it.moveToFirst()) it.getLong(0) else 0L }
            enc.rawExecSQL("PRAGMA main.user_version = $version;")
            enc.rawExecSQL("DETACH DATABASE plaintext;")
        } finally {
            enc.close()
        }
    }

    private data class Stats(
        val accounts: Long,
        val categories: Long,
        val transactions: Long,
        val sumCents: Long,
    )

    private fun readPlaintextStats(): Stats {
        val db = FrameworkDatabase.openDatabase(files.db.path, null, FrameworkDatabase.OPEN_READONLY)
        try {
            return readStats { sql -> db.rawQuery(sql, null) }
        } finally {
            db.close()
        }
    }

    // ademas de las cuentas comprueba la integridad de sqlcipher y las llaves foraneas
    private fun readEncryptedStats(passphrase: ByteArray): Stats {
        val db = CipherDatabase.openOrCreateDatabase(files.candidate, passphrase, null, null)
        try {
            db.rawQuery("PRAGMA integrity_check", null).use { c ->
                if (!c.moveToFirst() || c.getString(0) != "ok") throw MigrationValidationException("integrity check")
            }
            db.rawQuery("PRAGMA foreign_key_check", null).use { c ->
                if (c.moveToFirst()) throw MigrationValidationException("foreign key violation")
            }
            return readStats { sql -> db.rawQuery(sql, null) }
        } finally {
            db.close()
        }
    }

    private fun readStats(query: (String) -> Cursor): Stats {
        fun count(table: String): Long =
            query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getLong(0) }
        val sum = query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions")
            .use { it.moveToFirst(); it.getLong(0) }
        return Stats(count("accounts"), count("categories"), count("transactions"), sum)
    }

    // reemplazo atomico con respaldo para poder volver atras si algo falla
    private fun atomicReplace() {
        if (!files.db.renameTo(files.backup)) throw MigrationValidationException("backup rename failed")
        if (!files.candidate.renameTo(files.db)) {
            // no se pudo poner el cifrado, se restaura el original
            files.backup.renameTo(files.db)
            throw MigrationValidationException("candidate rename failed")
        }
        files.backup.delete()
        files.wal.delete()
        files.shm.delete()
        files.journal.delete()
    }
}
