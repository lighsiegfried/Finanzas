package com.kratt.finanzas.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.security.DatabaseFiles
import com.kratt.finanzas.data.security.DatabaseKeyManager
import com.kratt.finanzas.data.security.DatabasePassphraseProvider
import com.kratt.finanzas.data.security.SqlCipherNative
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// valida que un respaldo de la fase 2c (esquema 1) se pueda restaurar en la app con esquema 2
@RunWith(AndroidJUnit4::class)
class BackupCompatibilityTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val dbName = "bkpcompat.db"
    private val alias = "finanzas_bkpcompat_key"
    private val passphrase = ByteArray(32) { (it + 5).toByte() }
    private fun password() = "respaldo-2c-compatible".toByteArray()

    private lateinit var files: DatabaseFiles
    private lateinit var keyManager: DatabaseKeyManager
    private lateinit var manager: BackupManager
    private val backupFile get() = File(context.cacheDir, "v1-backup.mfinanzas")
    private val v1File get() = File(context.cacheDir, "v1-source.db")

    @Before
    fun setUp() {
        files = DatabaseFiles(context, dbName, "bkpcompat_key.bin", "bkpcompat.inprogress")
        keyManager = DatabaseKeyManager(alias)
        manager = BackupManager(
            context = context,
            files = files,
            keyManager = keyManager,
            passphraseProvider = DatabasePassphraseProvider(files.envelope, keyManager),
            snapshot = DatabaseSnapshot(context, files),
            nowMillis = { 1L },
        )
        cleanAll()
    }

    @After
    fun tearDown() = cleanAll()

    private fun cleanAll() {
        listOf(
            files.db, files.wal, files.shm, files.journal, files.envelope, files.marker,
            files.restoreTempDb, files.snapshotTempDb, files.dbRestoreBackup, files.restoreMarker,
            files.envelopeRestoreBackup, backupFile, v1File,
            File(v1File.path + "-wal"), File(v1File.path + "-shm"),
        ).forEach { it.delete() }
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
    }

    // crea una base cifrada autocontenible con el esquema de la version 1
    private fun createV1Encrypted() {
        SqlCipherNative.ensureLoaded()
        val db = CipherDatabase.openOrCreateDatabase(v1File, passphrase, null, null)
        try {
            // journal_mode devuelve un valor, se ejecuta con rawQuery para dejar la base en un solo archivo
            db.rawQuery("PRAGMA journal_mode=DELETE", null).use { it.moveToFirst() }
            db.execSQL(
                "CREATE TABLE accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, " +
                    "type TEXT NOT NULL, currencyCode TEXT NOT NULL, initialBalanceCents INTEGER NOT NULL, " +
                    "isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, " +
                    "transactionType TEXT NOT NULL, iconKey TEXT NOT NULL, isDefault INTEGER NOT NULL, " +
                    "isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, accountId INTEGER NOT NULL, " +
                    "categoryId INTEGER NOT NULL, type TEXT NOT NULL, amountCents INTEGER NOT NULL, description TEXT, " +
                    "transactionDate INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, " +
                    "FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT, " +
                    "FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
            )
            db.execSQL("INSERT INTO accounts VALUES (1,'Efectivo','CASH','GTQ',0,1,100)")
            db.execSQL("INSERT INTO accounts VALUES (2,'BAM','BANK','GTQ',50000,1,100)")
            db.execSQL("INSERT INTO categories VALUES (1,'Alimentación','EXPENSE','food',1,1,100)")
            db.execSQL("INSERT INTO transactions VALUES (1,1,1,'EXPENSE',12575,'Compra',20000,100,100)")
            db.execSQL("PRAGMA user_version = 1")
        } finally {
            db.close()
        }
    }

    @Test
    fun phase2cBackupRestoresIntoSchemaV2() {
        createV1Encrypted()
        val manifest = BackupManifest(
            roomSchemaVersion = 1,
            createdAtMillis = 1L,
            dbName = "finanzas.db",
            passphrase = passphrase.copyOf(),
            encryptedDb = v1File.readBytes(),
        )
        FileOutputStream(backupFile).use { PortableBackupCodec.write(it, password(), manifest) }

        when (val outcome = manager.prepareRestore(FileInputStream(backupFile), password())) {
            is RestoreOutcome.Valid -> {
                // la validacion abre con room y migra el respaldo v1 a v2 sin perder datos
                assertEquals(2L, outcome.candidate.stats.accounts)
                assertTrue(outcome.candidate.stats.transactions >= 1L)
                manager.discardRestore(outcome.candidate)
            }
            else -> fail("un respaldo 2c debe restaurarse, fue $outcome")
        }
    }
}
