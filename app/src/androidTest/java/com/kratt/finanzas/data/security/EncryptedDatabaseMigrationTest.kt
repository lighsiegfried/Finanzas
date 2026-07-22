package com.kratt.finanzas.data.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.DefaultDataCallback
import com.kratt.finanzas.data.local.DefaultDataSeeder
import com.kratt.finanzas.data.local.entity.TransactionEntity
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.TransactionType
import java.io.File
import java.security.KeyStore
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// pruebas de cifrado y migracion sobre archivos aislados para no tocar la base real
@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseMigrationTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val dbName = "migtest.db"
    private val alias = "finanzas_migtest_key"
    private lateinit var files: DatabaseFiles
    private lateinit var keyManager: DatabaseKeyManager
    private lateinit var bootstrap: EncryptedDatabaseBootstrap

    @Before
    fun setUp() {
        files = DatabaseFiles(context, dbName, "migtest_key.bin", "migtest.inprogress")
        keyManager = DatabaseKeyManager(alias)
        bootstrap = EncryptedDatabaseBootstrap(context, files, keyManager)
        cleanAll()
    }

    @After
    fun tearDown() {
        cleanAll()
    }

    private fun cleanAll() {
        listOf(files.db, files.wal, files.shm, files.journal, files.candidate, files.backup, files.envelope, files.marker)
            .forEach { it.delete() }
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
    }

    private fun headerIsPlaintext(file: File): Boolean {
        val head = ByteArray(16)
        file.inputStream().use { it.read(head) }
        return String(head, 0, 15, Charsets.US_ASCII) == "SQLite format 3"
    }

    private fun ready(): AppDatabase {
        val result = bootstrap.bootstrap()
        assertTrue(result is EncryptedDatabaseBootstrap.Result.Ready)
        return (result as EncryptedDatabaseBootstrap.Result.Ready).database
    }

    // crea una base de texto plano con varias cuentas, categorias y movimientos
    private fun seedPlaintextFixture() {
        val plain = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addCallback(DefaultDataCallback())
            .build()
        runBlocking {
            val efectivo = plain.accountDao().observeActive().first().first().id
            plain.accountDao().insert(
                DefaultDataSeeder.defaultAccount(1L).copy(name = "Banco", type = AccountType.BANK_ACCOUNT),
            )
            val expense = plain.categoryDao().observeActiveByType(TransactionType.EXPENSE).first()
            val income = plain.categoryDao().observeActiveByType(TransactionType.INCOME).first()
            val alimentacion = expense.first { it.name == "Alimentación" }.id
            val transporte = expense.first { it.name == "Transporte" }.id
            val salario = income.first { it.name == "Salario" }.id
            val venta = income.first { it.name == "Venta" }.id

            fun tx(cat: Long, type: TransactionType, cents: Long, date: LocalDate, desc: String) =
                TransactionEntity(
                    accountId = efectivo, categoryId = cat, type = type, amountCents = cents,
                    description = desc, transactionDate = date.toEpochDay(), createdAt = 1L, updatedAt = 1L,
                )
            plain.transactionDao().insert(tx(alimentacion, TransactionType.EXPENSE, 12_575, LocalDate.of(2026, 6, 15), "Café en la esquina"))
            plain.transactionDao().insert(tx(transporte, TransactionType.EXPENSE, 5_000, LocalDate.of(2026, 6, 20), "Camión al trabajo"))
            plain.transactionDao().insert(tx(salario, TransactionType.INCOME, 500_000, LocalDate.of(2026, 7, 1), "Salario mensual"))
            plain.transactionDao().insert(tx(alimentacion, TransactionType.EXPENSE, 25_000, LocalDate.of(2026, 7, 10), "Compras del niño"))
            plain.transactionDao().insert(tx(venta, TransactionType.INCOME, 15_000, LocalDate.of(2026, 7, 15), "Venta de pan"))
        }
        plain.close()
    }

    @Test
    fun newInstall_createsEncryptedDatabase_withSeedAndEnvelopeInNoBackup() {
        val db = ready()
        runBlocking {
            assertEquals(1, db.accountDao().observeActive().first().size)
            assertEquals(9, db.categoryDao().observeActiveByType(TransactionType.EXPENSE).first().size)
        }
        assertFalse("db must not be plaintext", headerIsPlaintext(files.db))
        assertTrue(files.envelope.exists())
        assertTrue(files.envelope.absolutePath.startsWith(context.noBackupFilesDir.absolutePath))
        db.close()

        // reabrir tras cerrar debe conservar los datos
        val reopened = ready()
        runBlocking { assertEquals(1, reopened.accountDao().observeActive().first().size) }
        reopened.close()
    }

    @Test
    fun wrongPassphrase_failsToOpen_andDoesNotDeleteDatabase() {
        ready().close()
        assertTrue(files.db.exists())
        val room = AppDatabase.build(context, ByteArray(32) { 0 }, files.db.name)
        assertThrows(Exception::class.java) { room.openHelper.readableDatabase }
        try { room.close() } catch (e: Exception) { }
        // la base cifrada sigue en su lugar, no se borro por la clave equivocada
        assertTrue(files.db.exists())
        assertFalse(headerIsPlaintext(files.db))
    }

    @Test
    fun plaintextMigration_preservesRowCountsAndAggregate() {
        seedPlaintextFixture()
        assertTrue(headerIsPlaintext(files.db))

        val db = ready()
        runBlocking {
            val accounts = db.accountDao().observeActive().first()
            val expense = db.categoryDao().observeActiveByType(TransactionType.EXPENSE).first()
            val income = db.categoryDao().observeActiveByType(TransactionType.INCOME).first()
            val rows = db.transactionDao().observeAllWithNames().first()
            assertEquals(2, accounts.size)
            assertEquals(14, expense.size + income.size)
            assertEquals(5, rows.size)
            assertEquals(557_575L, rows.sumOf { it.amountCents })
        }
        // la base migrada ya no es texto plano y no quedan artefactos temporales
        assertFalse(headerIsPlaintext(files.db))
        assertFalse(files.candidate.exists())
        assertFalse(files.backup.exists())
        assertFalse(files.marker.exists())
        db.close()
    }

    @Test
    fun failedCandidateValidation_preservesOriginalPlaintext() {
        seedPlaintextFixture()
        val migrator = PlaintextToEncryptedMigrator(files) { _, _ -> throw RuntimeException("boom") }
        assertThrows(Exception::class.java) { migrator.migrate(ByteArray(32) { 7 }) }
        // el original de texto plano queda intacto, no se reemplazo
        assertTrue(files.db.exists())
        assertTrue(headerIsPlaintext(files.db))
    }

    @Test
    fun interruptedMigration_recoversAndPreservesData() {
        seedPlaintextFixture()
        // simula una interrupcion: marcador presente y un candidato incompleto
        files.marker.writeBytes(byteArrayOf(1))
        files.candidate.writeBytes(byteArrayOf(9, 9, 9))

        val db = ready()
        runBlocking {
            assertEquals(5, db.transactionDao().observeAllWithNames().first().size)
        }
        assertFalse(headerIsPlaintext(files.db))
        assertFalse(files.marker.exists())
        db.close()
    }
}
