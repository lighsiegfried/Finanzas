package com.kratt.finanzas.data.backup

import android.content.Context
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.security.DatabaseFiles

// toma una copia estable de la base cifrada para empaquetarla en el respaldo
class DatabaseSnapshot(
    private val context: Context,
    private val files: DatabaseFiles,
) {
    // deja el wal en el archivo principal, copia los bytes cifrados y los valida
    fun capture(database: AppDatabase, passphrase: ByteArray): ByteArray {
        // el cursor debe avanzarse para que el checkpoint se ejecute de verdad
        database.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        val bytes = files.db.readBytes()
        files.snapshotTempDb.writeBytes(bytes)
        try {
            EncryptedDbInspector.inspect(files.snapshotTempDb, passphrase)
            EncryptedDbInspector.verifyWithRoom(context, files.snapshotTempDb, passphrase)
        } finally {
            files.snapshotTempDb.delete()
        }
        return bytes
    }
}
