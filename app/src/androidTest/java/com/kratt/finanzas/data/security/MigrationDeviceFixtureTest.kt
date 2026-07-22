package com.kratt.finanzas.data.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.FinanzasApplication
import com.kratt.finanzas.ManualDeviceTest
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.DefaultDataCallback
import com.kratt.finanzas.data.local.entity.TransactionEntity
import com.kratt.finanzas.di.DatabaseBootstrapState
import com.kratt.finanzas.domain.model.TransactionType
import java.security.KeyStore
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

// herramienta manual: deja la base real como texto plano para probar la migracion en el dispositivo
// no se limpia a proposito, se ejecuta con filtro de clase desde adb
@RunWith(AndroidJUnit4::class)
@ManualDeviceTest
class MigrationDeviceFixtureTest {

    @Test
    fun installPlaintextFixtureAtRealPath() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context as FinanzasApplication
        val start = System.currentTimeMillis()
        while (app.container.databaseState.value != DatabaseBootstrapState.READY) {
            if (System.currentTimeMillis() - start > 15_000) error("bootstrap timeout")
            Thread.sleep(50)
        }
        // cierra la base cifrada y borra todo para simular una instalacion vieja sin cifrar
        app.container.database.close()
        val files = DatabaseFiles(context, AppDatabase.NAME)
        listOf(files.db, files.wal, files.shm, files.journal, files.candidate, files.backup, files.envelope, files.marker)
            .forEach { it.delete() }
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(DatabaseKeyManager.DEFAULT_ALIAS)) ks.deleteEntry(DatabaseKeyManager.DEFAULT_ALIAS)

        // crea una base de texto plano con datos de prueba en la ruta real
        val plain = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addCallback(DefaultDataCallback())
            .build()
        runBlocking {
            val efectivo = plain.accountDao().observeActive().first().first().id
            val expense = plain.categoryDao().observeActiveByType(TransactionType.EXPENSE).first()
            val income = plain.categoryDao().observeActiveByType(TransactionType.INCOME).first()
            plain.transactionDao().insert(
                TransactionEntity(
                    accountId = efectivo, categoryId = expense.first { it.name == "Alimentación" }.id,
                    type = TransactionType.EXPENSE, amountCents = 12_575, description = "Compra antes de migrar",
                    transactionDate = LocalDate.of(2026, 7, 5).toEpochDay(), createdAt = 1L, updatedAt = 1L,
                ),
            )
            plain.transactionDao().insert(
                TransactionEntity(
                    accountId = efectivo, categoryId = income.first { it.name == "Salario" }.id,
                    type = TransactionType.INCOME, amountCents = 500_000, description = "Salario",
                    transactionDate = LocalDate.of(2026, 7, 1).toEpochDay(), createdAt = 1L, updatedAt = 1L,
                ),
            )
        }
        plain.close()
    }
}
