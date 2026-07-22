package com.kratt.finanzas.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.local.entity.TransactionEntity
import com.kratt.finanzas.data.security.SqlCipherNative
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.TransactionType
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// valida la migracion 1 a 2 sobre una base cifrada real, sin destruir datos
@RunWith(AndroidJUnit4::class)
class SchemaMigration1to2Test {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val dbName = "migv1test.db"
    private val passphrase = ByteArray(32) { (it + 1).toByte() }
    private val dbFile: File get() = context.getDatabasePath(dbName)

    @Before
    fun setUp() = cleanAll()

    @After
    fun tearDown() = cleanAll()

    private fun cleanAll() {
        listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm"), File(dbFile.path + "-journal"))
            .forEach { it.delete() }
    }

    // crea una base cifrada con el esquema y datos de la version 1
    private fun createV1Fixture() {
        SqlCipherNative.ensureLoaded()
        dbFile.parentFile?.mkdirs()
        val db = CipherDatabase.openOrCreateDatabase(dbFile, passphrase, null, null)
        try {
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
            db.execSQL("INSERT INTO accounts VALUES (2,'Banco','BANK','GTQ',500000,1,100)")
            db.execSQL("INSERT INTO categories VALUES (1,'Alimentación','EXPENSE','food',1,1,100)")
            db.execSQL("INSERT INTO categories VALUES (2,'Salario','INCOME','salary',1,1,100)")
            db.execSQL("INSERT INTO transactions VALUES (1,1,1,'EXPENSE',12575,'Compra',20000,100,100)")
            db.execSQL("INSERT INTO transactions VALUES (2,1,2,'INCOME',500000,'Salario',20001,100,100)")
            db.execSQL("PRAGMA user_version = 1")
        } finally {
            db.close()
        }
    }

    @Test
    fun migratesEncryptedV1ToV2_preservingData_andEnablingTransfers() {
        createV1Fixture()

        // abrir con room dispara la migracion 1 a 2 sobre el archivo cifrado
        val room = AppDatabase.build(context, passphrase.copyOf(), dbName)
        try {
            runBlocking {
                val accounts = room.accountDao().observeActive().first()
                assertEquals(2, accounts.size)
                val banco = accounts.first { it.name == "Banco" }
                // el tipo BANK legado se renombra a BANK_ACCOUNT
                assertEquals(AccountType.BANK_ACCOUNT, banco.type)
                // updatedAt se rellena a partir de createdAt
                assertEquals(banco.createdAt, banco.updatedAt)
                assertNull(banco.creditLimitCents)

                val expense = room.categoryDao().observeActiveByType(TransactionType.EXPENSE).first()
                assertTrue(expense.any { it.name == "Alimentación" })

                val existing = room.transactionDao().observeBetween(0, 30000).first()
                assertEquals(2, existing.size)
                assertTrue(existing.all { it.categoryId != null && it.destinationAccountId == null })

                // el esquema nuevo acepta una transferencia sin categoria
                room.transactionDao().insert(
                    TransactionEntity(
                        accountId = 1, destinationAccountId = 2, categoryId = null,
                        type = TransactionType.TRANSFER, amountCents = 5000, description = "Traslado",
                        transactionDate = 20002, createdAt = 200, updatedAt = 200,
                    ),
                )
                val transfer = room.transactionDao().observeBetween(20002, 20002).first().first()
                assertEquals(TransactionType.TRANSFER, transfer.type)
                assertEquals(2L, transfer.destinationAccountId)
                assertNull(transfer.categoryId)
                assertNotNull(transfer)
            }
        } finally {
            room.close()
        }
    }
}
