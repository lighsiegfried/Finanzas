package com.kratt.finanzas.data.backup

import android.content.Context
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.security.SqlCipherNative
import java.io.File
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase

// fallo al validar una base cifrada, el mensaje no lleva datos sensibles
class BackupValidationException(message: String) : Exception(message)

object EncryptedDbInspector {

    data class Stats(
        val accounts: Long,
        val categories: Long,
        val transactions: Long,
        val sumCents: Long,
    )

    // abre la base cifrada, revisa integridad y llaves foraneas y cuenta filas
    fun inspect(dbFile: File, passphrase: ByteArray): Stats {
        SqlCipherNative.ensureLoaded()
        val db = CipherDatabase.openOrCreateDatabase(dbFile, passphrase, null, null)
        try {
            db.rawQuery("PRAGMA integrity_check", null).use { c ->
                if (!c.moveToFirst() || c.getString(0) != "ok") throw BackupValidationException("integrity")
            }
            db.rawQuery("PRAGMA foreign_key_check", null).use { c ->
                if (c.moveToFirst()) throw BackupValidationException("foreign key")
            }
            fun count(table: String): Long =
                db.rawQuery("SELECT COUNT(*) FROM $table", null).use { it.moveToFirst(); it.getLong(0) }
            val sum = db.rawQuery("SELECT COALESCE(SUM(amountCents), 0) FROM transactions", null)
                .use { it.moveToFirst(); it.getLong(0) }
            return Stats(count("accounts"), count("categories"), count("transactions"), sum)
        } finally {
            db.close()
        }
    }

    // abre con room para validar el esquema y una consulta representativa
    fun verifyWithRoom(context: Context, dbFile: File, passphrase: ByteArray) {
        val room = AppDatabase.build(context, passphrase.copyOf(), dbFile.name)
        try {
            room.query("SELECT COUNT(*) FROM accounts", null).use { it.moveToFirst() }
        } finally {
            room.close()
        }
    }
}
