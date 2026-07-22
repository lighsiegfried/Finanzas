package com.kratt.finanzas.data.backup

import android.content.Context
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.security.DatabaseFiles
import com.kratt.finanzas.data.security.DatabaseKeyManager
import com.kratt.finanzas.data.security.DatabasePassphraseProvider
import java.io.File
import java.io.InputStream
import java.io.OutputStream

// candidato de restauracion ya validado, la frase y los adjuntos viven solo en memoria
class RestoreCandidate(
    val tempDb: File,
    val passphrase: ByteArray,
    val stats: EncryptedDbInspector.Stats,
    val createdAtMillis: Long,
    val attachments: List<BackupAttachment> = emptyList(),
)

sealed interface RestoreOutcome {
    data class Valid(val candidate: RestoreCandidate) : RestoreOutcome
    data object WrongPasswordOrCorrupt : RestoreOutcome
    data object Unsupported : RestoreOutcome
}

// arma los respaldos y ejecuta la restauracion no destructiva con reversion
class BackupManager(
    private val context: Context,
    private val files: DatabaseFiles,
    private val keyManager: DatabaseKeyManager,
    private val passphraseProvider: DatabasePassphraseProvider,
    private val snapshot: DatabaseSnapshot,
    private val nowMillis: () -> Long,
) {
    // crea el respaldo cifrado a partir de la base viva y devuelve la marca de tiempo
    // los adjuntos vienen ya descifrados y viajan protegidos por el cifrado del propio respaldo
    fun createBackup(
        output: OutputStream,
        password: ByteArray,
        database: AppDatabase,
        attachments: List<BackupAttachment> = emptyList(),
    ): Long {
        val passphrase = passphraseProvider.load()
        return try {
            val dbBytes = snapshot.capture(database, passphrase)
            val createdAt = nowMillis()
            val manifest = BackupManifest(
                roomSchemaVersion = BackupManifest.CURRENT_ROOM_SCHEMA,
                createdAtMillis = createdAt,
                dbName = files.db.name,
                passphrase = passphrase,
                encryptedDb = dbBytes,
                attachments = attachments,
            )
            PortableBackupCodec.write(output, password, manifest)
            createdAt
        } finally {
            passphrase.fill(0)
            attachments.forEach { it.plaintext.fill(0) }
        }
    }

    // lee y valida el respaldo, escribe solo los bytes cifrados a un temporal
    fun prepareRestore(input: InputStream, password: ByteArray): RestoreOutcome {
        val manifest = try {
            PortableBackupCodec.readManifest(input, password)
        } catch (e: BackupFormatException.UnsupportedVersion) {
            return RestoreOutcome.Unsupported
        } catch (e: BackupFormatException.UnsupportedSchema) {
            return RestoreOutcome.Unsupported
        } catch (e: BackupFormatException.UnsupportedKdf) {
            return RestoreOutcome.Unsupported
        } catch (e: BackupFormatException.UnsupportedEncryption) {
            return RestoreOutcome.Unsupported
        } catch (e: Exception) {
            // contrasena incorrecta, respaldo danado o formato invalido
            return RestoreOutcome.WrongPasswordOrCorrupt
        }
        files.restoreTempDb.writeBytes(manifest.encryptedDb)
        return try {
            val stats = EncryptedDbInspector.inspect(files.restoreTempDb, manifest.passphrase)
            EncryptedDbInspector.verifyWithRoom(context, files.restoreTempDb, manifest.passphrase)
            RestoreOutcome.Valid(
                RestoreCandidate(
                    files.restoreTempDb,
                    manifest.passphrase.copyOf(),
                    stats,
                    manifest.createdAtMillis,
                    manifest.attachments,
                ),
            )
        } catch (e: Exception) {
            files.restoreTempDb.delete()
            // el candidato no se entrega: limpia el contenido de los adjuntos en memoria
            manifest.attachments.forEach { it.plaintext.fill(0) }
            RestoreOutcome.WrongPasswordOrCorrupt
        } finally {
            manifest.passphrase.fill(0)
            manifest.encryptedDb.fill(0)
        }
    }

    // reemplaza la base actual por la del respaldo, conservando la original por si falla
    // writeAttachment recibe cada adjunto descifrado para volver a cifrarlo con la clave local destino
    fun commitRestore(
        candidate: RestoreCandidate,
        closeCurrentDatabase: () -> Unit,
        writeAttachment: (String, ByteArray) -> Unit = { _, _ -> },
    ) {
        files.restoreMarker.writeBytes(byteArrayOf(1))
        try {
            closeCurrentDatabase()
            // conserva la base y el sobre actuales como reversion
            if (files.db.exists()) files.db.copyTo(files.dbRestoreBackup, overwrite = true)
            if (files.envelope.exists()) files.envelope.copyTo(files.envelopeRestoreBackup, overwrite = true)
            // reemplaza la base y limpia los sidecars de la anterior
            candidate.tempDb.copyTo(files.db, overwrite = true)
            files.wal.delete(); files.shm.delete(); files.journal.delete()
            // envuelve la frase importada en un sobre local nuevo del dispositivo destino
            val envelope = keyManager.wrap(candidate.passphrase)
            writeEnvelope(files.envelope, envelope.encode())
            EncryptedDbInspector.verifyWithRoom(context, files.db, candidate.passphrase)
            // los datos financieros ya estan restaurados; los adjuntos se escriben en mejor esfuerzo
            // un adjunto que no se pueda escribir no corrompe los datos: su fila se limpia al abrir
            candidate.attachments.forEach { runCatching { writeAttachment(it.storedFileName, it.plaintext) } }
            // exito: elimina los artefactos de reversion y temporales
            files.dbRestoreBackup.delete()
            files.envelopeRestoreBackup.delete()
            candidate.tempDb.delete()
            files.restoreMarker.delete()
        } catch (e: Exception) {
            rollback()
            throw e
        } finally {
            candidate.passphrase.fill(0)
            candidate.attachments.forEach { it.plaintext.fill(0) }
        }
    }

    fun discardRestore(candidate: RestoreCandidate) {
        candidate.tempDb.delete()
        candidate.passphrase.fill(0)
        candidate.attachments.forEach { it.plaintext.fill(0) }
    }

    // vuelve a la base y al sobre originales si la restauracion no se completa
    private fun rollback() {
        if (files.dbRestoreBackup.exists()) {
            files.dbRestoreBackup.copyTo(files.db, overwrite = true)
            files.dbRestoreBackup.delete()
        }
        if (files.envelopeRestoreBackup.exists()) {
            files.envelopeRestoreBackup.copyTo(files.envelope, overwrite = true)
            files.envelopeRestoreBackup.delete()
        }
        files.restoreTempDb.delete()
        files.restoreMarker.delete()
    }

    private fun writeEnvelope(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }
}
