package com.kratt.finanzas.data.security

import android.content.Context
import com.kratt.finanzas.data.local.AppDatabase
import java.io.File

// arranca la base cifrada: crea, migra o pide recuperacion segun el estado real
class EncryptedDatabaseBootstrap(
    private val context: Context,
    private val files: DatabaseFiles = DatabaseFiles(context.applicationContext, AppDatabase.NAME),
    private val keyManager: DatabaseKeyManager = DatabaseKeyManager(),
) {
    private val appContext = context.applicationContext
    private val passphraseProvider = DatabasePassphraseProvider(files.envelope, keyManager)
    private val detector = DatabaseStateDetector(files.db, files.marker)
    private val lock = Any()

    sealed interface Result {
        data class Ready(val database: AppDatabase) : Result
        data object RecoveryRequired : Result
    }

    // arranca una sola vez, sincronizado para que no corran dos migraciones a la vez
    fun bootstrap(onMigrating: () -> Unit = {}): Result = synchronized(lock) {
        SqlCipherNative.ensureLoaded()
        try {
            resolvePendingRestore()
            resolvePendingMarker()
            when (detector.detect(passphraseProvider.envelopeExists(), keyManager.hasWrappingKey())) {
                DatabaseState.NEW_INSTALL -> openFresh()
                DatabaseState.PLAINTEXT_READY_FOR_MIGRATION -> {
                    onMigrating()
                    migrateThenOpen()
                }
                DatabaseState.ENCRYPTED_READY -> openEncrypted()
                DatabaseState.MIGRATION_IN_PROGRESS,
                DatabaseState.RECOVERY_REQUIRED,
                DatabaseState.CORRUPT_OR_UNSUPPORTED,
                -> Result.RecoveryRequired
            }
        } catch (e: Exception) {
            // ningun fallo borra datos, se pasa a recuperacion
            Result.RecoveryRequired
        }
    }

    // una restauracion interrumpida vuelve a la base original conservada
    private fun resolvePendingRestore() {
        if (!files.restoreMarker.exists()) return
        if (files.dbRestoreBackup.exists()) {
            files.dbRestoreBackup.copyTo(files.db, overwrite = true)
            files.dbRestoreBackup.delete()
            if (files.envelopeRestoreBackup.exists()) {
                files.envelopeRestoreBackup.copyTo(files.envelope, overwrite = true)
                files.envelopeRestoreBackup.delete()
            }
        }
        files.restoreTempDb.delete()
        files.restoreMarker.delete()
    }

    // resuelve una migracion interrumpida mirando el estado real de los archivos
    private fun resolvePendingMarker() {
        if (!files.marker.exists()) return
        when {
            // el cifrado ya quedo en su lugar, solo se limpia
            files.db.exists() && !detector.isPlaintext(files.db) -> {
                files.backup.delete()
                files.candidate.delete()
            }
            // no se alcanzo a poner el cifrado, se restaura el original
            !files.db.exists() && files.backup.exists() -> {
                files.backup.renameTo(files.db)
                files.candidate.delete()
            }
            // el texto plano sigue, se descarta el candidato incompleto
            else -> {
                files.candidate.delete()
                files.backup.delete()
            }
        }
        files.marker.delete()
    }

    // instalacion nueva: crea el sobre si falta y abre una base cifrada con semilla
    private fun openFresh(): Result {
        val passphrase = passphraseProvider.getOrCreate()
        return try {
            val db = AppDatabase.build(appContext, passphrase.copyOf(), files.db.name)
            db.openHelper.writableDatabase
            Result.Ready(db)
        } finally {
            passphrase.fill(0)
        }
    }

    // migra el texto plano y luego abre la base ya cifrada
    private fun migrateThenOpen(): Result {
        val passphrase = passphraseProvider.getOrCreate()
        return try {
            val migrator = PlaintextToEncryptedMigrator(files, ::validateWithRoom)
            migrator.migrate(passphrase)
            val db = AppDatabase.build(appContext, passphrase.copyOf(), files.db.name)
            db.openHelper.readableDatabase
            Result.Ready(db)
        } finally {
            passphrase.fill(0)
        }
    }

    // abre la base ya cifrada, si la frase o la base fallan se pide recuperacion
    private fun openEncrypted(): Result {
        val passphrase = try {
            passphraseProvider.load()
        } catch (e: Exception) {
            return Result.RecoveryRequired
        }
        return try {
            val db = AppDatabase.build(appContext, passphrase.copyOf(), files.db.name)
            db.openHelper.readableDatabase
            Result.Ready(db)
        } catch (e: Exception) {
            Result.RecoveryRequired
        } finally {
            passphrase.fill(0)
        }
    }

    // abre el candidato con la configuracion real de room para validar el esquema
    private fun validateWithRoom(candidate: File, passphrase: ByteArray) {
        val room = AppDatabase.build(appContext, passphrase.copyOf(), candidate.name)
        try {
            room.query("SELECT COUNT(*) FROM accounts", null).use { it.moveToFirst() }
        } finally {
            room.close()
        }
    }
}
