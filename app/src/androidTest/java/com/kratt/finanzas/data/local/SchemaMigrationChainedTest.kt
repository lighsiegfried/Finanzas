package com.kratt.finanzas.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.security.SqlCipherNative
import com.kratt.finanzas.domain.model.AccountType
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// valida la cadena completa de migraciones 1 a 6 sobre una base cifrada real, sin destruir datos
// abrir con room dispara MIGRATION_1_2 hasta MIGRATION_5_6 en secuencia
@RunWith(AndroidJUnit4::class)
class SchemaMigrationChainedTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val dbName = "migchain16test.db"
    private val passphrase = ByteArray(32) { (it + 9).toByte() }
    private val dbFile: File get() = context.getDatabasePath(dbName)

    @Before fun setUp() = cleanAll()
    @After fun tearDown() = cleanAll()

    private fun cleanAll() {
        listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm"), File(dbFile.path + "-journal")).forEach { it.delete() }
    }

    // base cifrada con el esquema y datos de la version 1 original
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
            db.execSQL("INSERT INTO transactions VALUES (1,1,1,'EXPENSE',12575,'Compra',20000,100,100)")
            db.execSQL("INSERT INTO transactions VALUES (2,2,1,'INCOME',500000,'Salario',20001,100,100)")
            db.execSQL("PRAGMA user_version = 1")
        } finally {
            db.close()
        }
    }

    @Test
    fun migratesEncryptedV1ToV6_chained_preservingData() {
        createV1Fixture()
        val room = AppDatabase.build(context, passphrase.copyOf(), dbName)
        try {
            runBlocking {
                // datos de la v1 preservados a traves de toda la cadena
                val accounts = room.accountDao().observeAll().first()
                assertEquals(2, accounts.size)
                // el tipo BANK legado se renombro a BANK_ACCOUNT en la migracion 1 a 2
                assertEquals(AccountType.BANK_ACCOUNT, accounts.first { it.name == "Banco" }.type)
                assertEquals(12_575L, room.transactionDao().findById(1)!!.amountCents)
                assertEquals(500_000L, room.transactionDao().findById(2)!!.amountCents)
                // la tabla de adjuntos de la v6 existe tras la cadena completa
                assertEquals(0, room.attachmentDao().observeTotalCount().first())
            }
        } finally {
            room.close()
        }
    }
}
