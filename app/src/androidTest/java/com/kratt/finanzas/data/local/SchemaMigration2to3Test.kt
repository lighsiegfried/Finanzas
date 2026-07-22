package com.kratt.finanzas.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.local.entity.InstallmentOccurrenceEntity
import com.kratt.finanzas.data.local.entity.InstallmentPlanEntity
import com.kratt.finanzas.data.local.entity.RecurringOccurrenceEntity
import com.kratt.finanzas.data.local.entity.RecurringTemplateEntity
import com.kratt.finanzas.data.security.SqlCipherNative
import com.kratt.finanzas.domain.model.InstallmentFrequency
import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus
import com.kratt.finanzas.domain.model.InstallmentStatus
import com.kratt.finanzas.domain.model.PostingMode
import com.kratt.finanzas.domain.model.RecurrenceType
import com.kratt.finanzas.domain.model.RecurringOccurrenceStatus
import com.kratt.finanzas.domain.model.TransactionType
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// valida la migracion 2 a 3 sobre una base cifrada real, preservando datos y creando las tablas nuevas
@RunWith(AndroidJUnit4::class)
class SchemaMigration2to3Test {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val dbName = "migv2test.db"
    private val passphrase = ByteArray(32) { (it + 3).toByte() }
    private val dbFile: File get() = context.getDatabasePath(dbName)

    @Before
    fun setUp() = cleanAll()

    @After
    fun tearDown() = cleanAll()

    private fun cleanAll() {
        listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm"), File(dbFile.path + "-journal"))
            .forEach { it.delete() }
    }

    // crea una base cifrada con el esquema y datos de la version 2
    private fun createV2Fixture() {
        SqlCipherNative.ensureLoaded()
        dbFile.parentFile?.mkdirs()
        val db = CipherDatabase.openOrCreateDatabase(dbFile, passphrase, null, null)
        try {
            db.execSQL(
                "CREATE TABLE accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, " +
                    "type TEXT NOT NULL, currencyCode TEXT NOT NULL, initialBalanceCents INTEGER NOT NULL, " +
                    "creditLimitCents INTEGER, lastFourDigits TEXT, description TEXT, isActive INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, " +
                    "transactionType TEXT NOT NULL, iconKey TEXT NOT NULL, colorKey TEXT, isDefault INTEGER NOT NULL, " +
                    "isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, accountId INTEGER NOT NULL, " +
                    "destinationAccountId INTEGER, categoryId INTEGER, type TEXT NOT NULL, amountCents INTEGER NOT NULL, " +
                    "description TEXT, transactionDate INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, " +
                    "FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT, " +
                    "FOREIGN KEY(destinationAccountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT, " +
                    "FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
            )
            db.execSQL("CREATE INDEX index_transactions_accountId ON transactions(accountId)")
            db.execSQL("CREATE INDEX index_transactions_destinationAccountId ON transactions(destinationAccountId)")
            db.execSQL("CREATE INDEX index_transactions_categoryId ON transactions(categoryId)")
            db.execSQL("CREATE INDEX index_transactions_transactionDate ON transactions(transactionDate)")
            db.execSQL("INSERT INTO accounts VALUES (1,'BAM','BANK_ACCOUNT','GTQ',50000,NULL,NULL,NULL,1,100,100)")
            db.execSQL("INSERT INTO categories VALUES (1,'Compras','EXPENSE','shopping',NULL,1,1,100,100)")
            db.execSQL("INSERT INTO transactions VALUES (1,1,NULL,1,'EXPENSE',12800,'Compra',20000,100,100)")
            db.execSQL("PRAGMA user_version = 2")
        } finally {
            db.close()
        }
    }

    @Test
    fun migratesEncryptedV2ToV3_preservingData_andCreatingNewTables() {
        createV2Fixture()

        val room = AppDatabase.build(context, passphrase.copyOf(), dbName)
        try {
            runBlocking {
                // datos previos preservados
                assertEquals(1, room.accountDao().observeActive().first().size)
                val existing = room.transactionDao().findById(1)!!
                assertEquals(12_800L, existing.amountCents)
                assertNull(existing.originKey)

                // la columna nueva originKey funciona
                val taggedId = room.transactionDao().insert(
                    existing.copy(id = 0, originKey = "installment:1"),
                )
                assertEquals("installment:1", room.transactionDao().findById(taggedId)!!.originKey)

                // las tablas de cuotas existen y respetan las llaves foraneas
                val planId = room.installmentDao().insertPlan(
                    InstallmentPlanEntity(
                        name = "Monitor", accountId = 1, categoryId = 1, totalAmountCents = 153_600,
                        installmentCount = 12, installmentAmountCents = 12_800, firstDueDate = 20_000,
                        frequency = InstallmentFrequency.MONTHLY, paidInstallments = 0,
                        status = InstallmentStatus.ACTIVE, createdAt = 1, updatedAt = 1,
                    ),
                )
                room.installmentDao().insertOccurrences(
                    listOf(
                        InstallmentOccurrenceEntity(
                            installmentPlanId = planId, sequenceNumber = 1, dueDate = 20_000, amountCents = 12_800,
                            status = InstallmentOccurrenceStatus.PENDING, createdAt = 1, updatedAt = 1,
                        ),
                    ),
                )
                assertEquals(1, room.installmentDao().observeOccurrences(planId).first().size)

                // las tablas recurrentes existen
                val templateId = room.recurringDao().insertTemplate(
                    RecurringTemplateEntity(
                        name = "Internet", transactionType = TransactionType.EXPENSE, accountId = 1, categoryId = 1,
                        amountCents = 20_000, recurrenceType = RecurrenceType.MONTHLY, interval = 1,
                        startDate = 20_000, nextOccurrenceDate = 20_000, postingMode = PostingMode.REQUIRE_CONFIRMATION,
                        isActive = true, createdAt = 1, updatedAt = 1,
                    ),
                )
                room.recurringDao().insertOccurrence(
                    RecurringOccurrenceEntity(
                        recurringTemplateId = templateId, scheduledDate = 20_000, amountCents = 20_000,
                        status = RecurringOccurrenceStatus.PENDING, createdAt = 1, updatedAt = 1,
                    ),
                )
                assertTrue(room.recurringDao().observeOccurrences(templateId).first().isNotEmpty())
            }
        } finally {
            room.close()
        }
    }
}
