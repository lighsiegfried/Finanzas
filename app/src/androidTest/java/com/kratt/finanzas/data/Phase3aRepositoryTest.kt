package com.kratt.finanzas.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.DefaultDataCallback
import com.kratt.finanzas.data.repository.AccountRepositoryImpl
import com.kratt.finanzas.data.repository.CategoryRepositoryImpl
import com.kratt.finanzas.data.repository.TransactionRepositoryImpl
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.AccountBalanceCalculator
import com.kratt.finanzas.domain.usecase.AccountTotalsCalculator
import com.kratt.finanzas.domain.usecase.SaveTransferUseCase
import com.kratt.finanzas.domain.usecase.SummaryCalculator
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// pruebas de integracion de la fase 3a sobre una base en memoria con el mismo esquema
@RunWith(AndroidJUnit4::class)
class Phase3aRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var accounts: AccountRepositoryImpl
    private lateinit var categories: CategoryRepositoryImpl
    private lateinit var transactions: TransactionRepositoryImpl
    private lateinit var saveTransfer: SaveTransferUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(DefaultDataCallback())
            .build()
        accounts = AccountRepositoryImpl(db.accountDao()) { 1L }
        categories = CategoryRepositoryImpl(db.categoryDao()) { 1L }
        transactions = TransactionRepositoryImpl(db.transactionDao())
        saveTransfer = SaveTransferUseCase(transactions) { 1L }
    }

    @After
    fun tearDown() = db.close()

    private fun account(name: String, type: AccountType, initial: Long = 0L, limit: Long? = null) = Account(
        id = 0L, name = name, type = type, currencyCode = "GTQ",
        initialBalanceCents = initial, isActive = true, creditLimitCents = limit,
    )

    private suspend fun balanceOf(accountId: Long): Long {
        val all = transactions.observeAllTransactions().first()
        val totals = AccountTotalsCalculator.totalsFor(accountId, all)
        val account = accounts.findById(accountId)!!
        return AccountBalanceCalculator.calculate(account, totals).currentBalanceCents
    }

    private suspend fun expenseCategoryId(): Long =
        categories.observeActiveByType(TransactionType.EXPENSE).first().first().id

    @Test
    fun createAccounts_ofDifferentTypes() = runBlocking {
        accounts.insert(account("Efectivo caja", AccountType.CASH, 10_000))
        accounts.insert(account("BAM", AccountType.BANK_ACCOUNT, 50_000))
        accounts.insert(account("Visa", AccountType.CREDIT_CARD, 0, limit = 200_000))
        val all = accounts.observeAllAccounts().first()
        assertTrue(all.any { it.name == "BAM" && it.type == AccountType.BANK_ACCOUNT })
        assertTrue(all.any { it.name == "Visa" && it.type == AccountType.CREDIT_CARD })
    }

    @Test
    fun editAndDeactivateAccount() = runBlocking {
        val id = accounts.insert(account("Ahorro", AccountType.SAVINGS, 1_000))
        val loaded = accounts.findById(id)!!
        accounts.update(loaded.copy(name = "Ahorro familiar"))
        assertEquals("Ahorro familiar", accounts.findById(id)!!.name)

        accounts.setActive(id, false)
        assertFalse(accounts.observeActiveAccounts().first().any { it.id == id })
        assertTrue(accounts.observeAllAccounts().first().any { it.id == id })
    }

    @Test
    fun createAndDeactivateCategory_keepsHistoryButHidesFromActive() = runBlocking {
        val id = categories.insert(
            Category(id = 0L, name = "Regalos", transactionType = TransactionType.EXPENSE, iconKey = "other", isDefault = false, isActive = true),
        )
        assertTrue(categories.observeActiveByType(TransactionType.EXPENSE).first().any { it.id == id })
        categories.setActive(id, false)
        assertFalse(categories.observeActiveByType(TransactionType.EXPENSE).first().any { it.id == id })
        // sigue existiendo para conservar el historial
        assertTrue(categories.observeAllByType(TransactionType.EXPENSE).first().any { it.id == id })
    }

    @Test
    fun transferMovesBothBalances_andDoesNotAffectIncomeOrExpense() = runBlocking {
        val source = accounts.insert(account("Origen", AccountType.CASH, 100_000))
        val destination = accounts.insert(account("Destino", AccountType.BANK_ACCOUNT, 20_000))

        saveTransfer(0L, source, destination, 30_000, LocalDate.of(2026, 7, 10), "Traslado", null)

        assertEquals(70_000L, balanceOf(source))
        assertEquals(50_000L, balanceOf(destination))

        // el resumen del mes no cuenta la transferencia como ingreso ni gasto
        val monthly = transactions.observeMonthly(YearMonth.of(2026, 7)).first()
        val summary = SummaryCalculator.calculate(monthly)
        assertEquals(0L, summary.incomeCents)
        assertEquals(0L, summary.expenseCents)
    }

    @Test
    fun editingAmount_updatesAccountBalance() = runBlocking {
        val id = accounts.insert(account("Gastos", AccountType.CASH, 100_000))
        val category = expenseCategoryId()
        val txId = transactions.insert(
            Transaction(0L, id, TransactionType.EXPENSE, 10_000, null, LocalDate.of(2026, 7, 1), 1L, 1L, categoryId = category),
        )
        assertEquals(90_000L, balanceOf(id))

        val existing = transactions.findById(txId)!!
        transactions.update(existing.copy(amountCents = 25_000, updatedAtMillis = 2L))
        assertEquals(75_000L, balanceOf(id))
    }

    @Test
    fun deletingMovement_restoresBalance() = runBlocking {
        val id = accounts.insert(account("Caja", AccountType.CASH, 40_000))
        val category = expenseCategoryId()
        val txId = transactions.insert(
            Transaction(0L, id, TransactionType.EXPENSE, 15_000, null, LocalDate.of(2026, 7, 1), 1L, 1L, categoryId = category),
        )
        assertEquals(25_000L, balanceOf(id))
        transactions.delete(transactions.findById(txId)!!)
        assertEquals(40_000L, balanceOf(id))
    }
}
