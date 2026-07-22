package com.kratt.finanzas.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.entity.TransactionEntity
import com.kratt.finanzas.data.security.DatabaseFiles
import com.kratt.finanzas.data.security.DatabaseKeyManager
import com.kratt.finanzas.data.security.DatabasePassphraseProvider
import com.kratt.finanzas.data.security.EncryptedDatabaseBootstrap
import com.kratt.finanzas.domain.model.TransactionType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// pruebas del motor de respaldo sobre archivos aislados, sin tocar la base real de la app
@RunWith(AndroidJUnit4::class)
class BackupManagerInstrumentedTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val dbName = "bkptest.db"
    private val alias = "finanzas_bkptest_key"
    private val secretDescription = "Cafe secreto 987654 unico"

    private lateinit var files: DatabaseFiles
    private lateinit var keyManager: DatabaseKeyManager
    private lateinit var bootstrap: EncryptedDatabaseBootstrap
    private lateinit var manager: BackupManager
    private val backupFile get() = File(context.cacheDir, "bkptest-backup.mfinanzas")

    private fun password() = "respaldo-seguro-123".toByteArray()

    @Before
    fun setUp() {
        files = DatabaseFiles(context, dbName, "bkptest_key.bin", "bkptest.inprogress")
        keyManager = DatabaseKeyManager(alias)
        bootstrap = EncryptedDatabaseBootstrap(context, files, keyManager)
        manager = BackupManager(
            context = context,
            files = files,
            keyManager = keyManager,
            passphraseProvider = DatabasePassphraseProvider(files.envelope, keyManager),
            snapshot = DatabaseSnapshot(context, files),
            nowMillis = { 1_700_000_000_000L },
        )
        cleanAll()
    }

    @After
    fun tearDown() = cleanAll()

    private fun cleanAll() {
        listOf(
            files.db, files.wal, files.shm, files.journal, files.candidate, files.backup,
            files.envelope, files.marker, files.restoreTempDb, files.snapshotTempDb,
            files.dbRestoreBackup, files.restoreMarker, files.envelopeRestoreBackup, backupFile,
        ).forEach { it.delete() }
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
    }

    private fun ready(): AppDatabase {
        val result = bootstrap.bootstrap()
        assertTrue(result is EncryptedDatabaseBootstrap.Result.Ready)
        return (result as EncryptedDatabaseBootstrap.Result.Ready).database
    }

    private fun seedOne(db: AppDatabase, description: String) = runBlocking {
        val account = db.accountDao().observeActive().first().first().id
        val category = db.categoryDao().observeActiveByType(TransactionType.EXPENSE).first().first().id
        db.transactionDao().insert(
            TransactionEntity(
                accountId = account, categoryId = category, type = TransactionType.EXPENSE,
                amountCents = 12_345, description = description,
                transactionDate = LocalDate.of(2026, 7, 1).toEpochDay(), createdAt = 1L, updatedAt = 1L,
            ),
        )
    }

    private fun writeBackup(db: AppDatabase): Long =
        FileOutputStream(backupFile).use { manager.createBackup(it, password(), db) }

    private fun contains(haystack: ByteArray, needle: String): Boolean {
        val n = needle.toByteArray(Charsets.UTF_8)
        if (n.isEmpty() || haystack.size < n.size) return false
        outer@ for (i in 0..haystack.size - n.size) {
            for (j in n.indices) if (haystack[i + j] != n[j]) continue@outer
            return true
        }
        return false
    }

    @Test
    fun createBackup_hasMagic_andNoPlaintextData() {
        val db = ready()
        seedOne(db, secretDescription)
        val createdAt = writeBackup(db)
        db.close()

        assertEquals(1_700_000_000_000L, createdAt)
        val bytes = backupFile.readBytes()
        // encabezado publico con la firma FZBK
        assertArrayEquals(byteArrayOf(0x46, 0x5A, 0x42, 0x4B), bytes.copyOfRange(0, 4))
        // nada del contenido financiero aparece en claro
        assertFalse(contains(bytes, secretDescription))
        assertFalse(contains(bytes, "Efectivo"))
        assertFalse(contains(bytes, "SQLite format 3"))
        // ni rastro de preferencias no relacionadas
        assertFalse(contains(bytes, "has_backup"))
    }

    @Test
    fun prepareRestore_validPassword_reportsStatsFromBackup() {
        val db = ready()
        seedOne(db, secretDescription)
        writeBackup(db)
        db.close()

        when (val outcome = manager.prepareRestore(FileInputStream(backupFile), password())) {
            is RestoreOutcome.Valid -> {
                assertEquals(1L, outcome.candidate.stats.accounts)
                assertTrue(outcome.candidate.stats.transactions >= 1L)
                assertEquals(1_700_000_000_000L, outcome.candidate.createdAtMillis)
                manager.discardRestore(outcome.candidate)
            }
            else -> fail("se esperaba Valid, fue $outcome")
        }
    }

    @Test
    fun prepareRestore_wrongPassword_isNonDestructive() {
        val db = ready()
        seedOne(db, secretDescription)
        writeBackup(db)
        db.close()
        val original = files.db.readBytes()

        val outcome = manager.prepareRestore(FileInputStream(backupFile), "otra-clave-larga-1".toByteArray())
        assertTrue(outcome is RestoreOutcome.WrongPasswordOrCorrupt)
        // la base original sigue intacta y no queda temporal
        assertArrayEquals(original, files.db.readBytes())
        assertFalse(files.restoreTempDb.exists())
    }

    @Test
    fun prepareRestore_corruptFile_isNonDestructive() {
        val db = ready()
        seedOne(db, secretDescription)
        writeBackup(db)
        db.close()
        val original = files.db.readBytes()
        // corrompe un byte del payload cifrado
        val corrupt = backupFile.readBytes()
        corrupt[BackupHeader.SIZE + 3] = (corrupt[BackupHeader.SIZE + 3].toInt() xor 0x01).toByte()
        backupFile.writeBytes(corrupt)

        val outcome = manager.prepareRestore(FileInputStream(backupFile), password())
        assertTrue(outcome is RestoreOutcome.WrongPasswordOrCorrupt)
        assertArrayEquals(original, files.db.readBytes())
    }

    @Test
    fun commitRestore_replacesData_writesNewEnvelope_andCleansUp() {
        val db = ready()
        seedOne(db, secretDescription)
        writeBackup(db)
        db.close()
        val envelopeBefore = files.envelope.readBytes()

        val outcome = manager.prepareRestore(FileInputStream(backupFile), password())
        assertTrue(outcome is RestoreOutcome.Valid)
        val candidate = (outcome as RestoreOutcome.Valid).candidate
        manager.commitRestore(candidate, closeCurrentDatabase = { /* base ya cerrada */ })

        // se escribio un sobre local nuevo en el destino
        val envelopeAfter = files.envelope.readBytes()
        assertFalse(envelopeBefore.contentEquals(envelopeAfter))
        // los artefactos de reversion y temporales quedan limpios
        assertFalse(files.dbRestoreBackup.exists())
        assertFalse(files.envelopeRestoreBackup.exists())
        assertFalse(files.restoreMarker.exists())
        assertFalse(files.restoreTempDb.exists())

        // reabrir y confirmar que los datos restaurados son legibles
        val reopened = ready()
        runBlocking { assertEquals(1, reopened.accountDao().observeActive().first().size) }
        reopened.close()
    }

    @Test
    fun interruptedRestore_recoversOriginalOnBootstrap() {
        val db = ready()
        seedOne(db, secretDescription)
        db.close()
        val original = files.db.readBytes()

        // simula una restauracion interrumpida a mitad del reemplazo
        files.dbRestoreBackup.writeBytes(original)
        files.restoreMarker.writeBytes(byteArrayOf(1))
        files.db.writeBytes(byteArrayOf(9, 9, 9)) // base a medias, corrupta
        files.restoreTempDb.writeBytes(byteArrayOf(7))

        val recovered = ready()
        runBlocking { assertEquals(1, recovered.accountDao().observeActive().first().size) }
        recovered.close()
        assertFalse(files.restoreMarker.exists())
        assertFalse(files.restoreTempDb.exists())
    }
}
