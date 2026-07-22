package com.kratt.finanzas.data.security

import android.content.Context
import java.io.File

// rutas de la base y sus archivos auxiliares en almacenamiento interno
class DatabaseFiles(
    context: Context,
    dbName: String,
    envelopeName: String = "db_key_v1.bin",
    markerName: String = "migration.inprogress",
) {
    val db: File = context.getDatabasePath(dbName)
    private val dir: File = db.parentFile ?: context.filesDir
    val wal = File(dir, "$dbName-wal")
    val shm = File(dir, "$dbName-shm")
    val journal = File(dir, "$dbName-journal")
    // candidato cifrado y respaldo del original, en la misma carpeta para renombrar atomico
    val candidate = File(dir, "$dbName.enc.tmp")
    val backup = File(dir, "$dbName.migbak")

    // artefactos de la restauracion de un respaldo portable
    val restoreTempDb = File(dir, "$dbName.restore.tmp")
    val snapshotTempDb = File(dir, "$dbName.snapshot.tmp")
    val dbRestoreBackup = File(dir, "$dbName.restorebak")

    private val securityDir = File(context.noBackupFilesDir, "security").apply { mkdirs() }
    val envelope = File(securityDir, envelopeName)
    val marker = File(securityDir, markerName)
    val restoreMarker = File(securityDir, "restore.inprogress")
    val envelopeRestoreBackup = File(securityDir, "$envelopeName.restorebak")
}
