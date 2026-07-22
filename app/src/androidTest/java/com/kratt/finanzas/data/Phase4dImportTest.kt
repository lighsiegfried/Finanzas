package com.kratt.finanzas.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.csv.CsvImporter
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.repository.AccountRepositoryImpl
import com.kratt.finanzas.data.repository.CategoryRepositoryImpl
import com.kratt.finanzas.data.repository.TransactionRepositoryImpl
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.AccountBalanceCalculator
import com.kratt.finanzas.domain.usecase.AccountTotalsCalculator
import com.kratt.finanzas.domain.usecase.ReportAggregator
import com.kratt.finanzas.domain.usecase.csv.ImportedMovement
import java.io.ByteArrayInputStream
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// pruebas de integracion de la importacion csv: transaccion atomica, reversion y cuadre
@RunWith(AndroidJUnit4::class)
class Phase4dImportTest {

    private lateinit var db: AppDatabase
    private lateinit var accounts: AccountRepositoryImpl
    private lateinit var categories: CategoryRepositoryImpl
    private lateinit var transactions: TransactionRepositoryImpl
    private lateinit var importer: CsvImporter

    private val header = "tipo,fecha,descripcion,monto,cuenta,categoria,cuenta_destino"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // sin datos por defecto para controlar por completo cuentas y categorias del escenario
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        accounts = AccountRepositoryImpl(db.accountDao()) { 1L }
        categories = CategoryRepositoryImpl(db.categoryDao()) { 1L }
        transactions = TransactionRepositoryImpl(db.transactionDao())
        importer = CsvImporter(db, accounts, categories, transactions) { 1L }
    }

    @After
    fun tearDown() = db.close()

    private fun seed() = runBlocking {
        accounts.insert(Account(0L, "Efectivo", AccountType.CASH, "GTQ", 0L, true))
        accounts.insert(Account(0L, "Ahorro", AccountType.SAVINGS, "GTQ", 0L, true))
        categories.insert(Category(0L, "Alimentacion", TransactionType.EXPENSE, "food", false, true))
        categories.insert(Category(0L, "Salario", TransactionType.INCOME, "work", false, true))
    }

    private fun preview(csv: String) = runBlocking { importer.preview(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))) }

    @Test
    fun importValidCsv_insertsMovements_andReconciles() = runBlocking {
        seed()
        val csv = buildString {
            appendLine(header)
            appendLine("ingreso,2026-07-01,Salario,8000.00,Efectivo,Salario,")
            appendLine("gasto,2026-07-05,Almuerzo,85.00,Efectivo,Alimentacion,")
            appendLine("transferencia,2026-07-06,Ahorro,500.00,Efectivo,,Ahorro")
        }
        val p = preview(csv)
        assertEquals(3, p.validCount)
        assertEquals(0, p.errorCount)
        assertEquals(0, p.duplicateCount)

        val imported = importer.commit(p.valid)
        assertEquals(3, imported)

        val all = transactions.observeAllTransactions().first()
        val ie = ReportAggregator.incomeExpense(all)
        // la transferencia no cuenta como ingreso ni gasto
        assertEquals(800_000L, ie.incomeCents)
        assertEquals(8_500L, ie.expenseCents)

        val efectivo = accounts.observeAllAccounts().first().first { it.name == "Efectivo" }
        val ahorro = accounts.observeAllAccounts().first().first { it.name == "Ahorro" }
        // efectivo: 800000 - 8500 - 50000 = 741500 ; ahorro: +50000
        assertEquals(741_500L, balanceOf(efectivo.id, all))
        assertEquals(50_000L, balanceOf(ahorro.id, all))
    }

    @Test
    fun commit_rollsBackEverything_onFatalError() = runBlocking {
        seed()
        val efectivo = accounts.observeAllAccounts().first().first { it.name == "Efectivo" }.id
        val good = ImportedMovement(1, TransactionType.EXPENSE, LocalDate.of(2026, 7, 5), "ok", 8_500L, efectivo, categoryOf("Alimentacion"), null)
        // cuenta inexistente fuerza una violacion de llave foranea a mitad de la transaccion
        val bad = ImportedMovement(2, TransactionType.EXPENSE, LocalDate.of(2026, 7, 6), "bad", 1_000L, 999_999L, categoryOf("Alimentacion"), null)

        val threw = try {
            importer.commit(listOf(good, bad))
            false
        } catch (e: Exception) {
            true
        }
        assertEquals(true, threw)
        // como se revierte todo, no queda ningun movimiento insertado
        assertEquals(0, transactions.observeAllTransactions().first().size)
    }

    private fun categoryOf(name: String): Long = runBlocking {
        categories.observeActiveByType(TransactionType.EXPENSE).first().first { it.name == name }.id
    }

    private suspend fun balanceOf(accountId: Long, all: List<com.kratt.finanzas.domain.model.Transaction>): Long {
        val totals = AccountTotalsCalculator.totalsFor(accountId, all)
        val account = accounts.findById(accountId)!!
        return AccountBalanceCalculator.calculate(account, totals).currentBalanceCents
    }
}
