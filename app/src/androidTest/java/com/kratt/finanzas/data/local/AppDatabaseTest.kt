package com.kratt.finanzas.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.local.entity.AccountEntity
import com.kratt.finanzas.data.local.entity.TransactionEntity
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.MonthRange
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        // base en memoria con los mismos datos iniciales que la app real
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(DefaultDataCallback())
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // busca la cuenta efectivo y una categoria de gasto ya sembradas
    private suspend fun seededIds(): Pair<Long, Long> {
        val account = db.accountDao().observeActive().first().first()
        val category = db.categoryDao().observeActiveByType(TransactionType.EXPENSE).first()
            .first { it.name == "Alimentación" }
        return account.id to category.id
    }

    private fun transaction(
        accountId: Long,
        categoryId: Long,
        type: TransactionType,
        cents: Long,
        epochDay: Long,
        description: String? = null,
    ): TransactionEntity = TransactionEntity(
        accountId = accountId,
        categoryId = categoryId,
        type = type,
        amountCents = cents,
        description = description,
        transactionDate = epochDay,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun defaultAccount_isSeededOnCreate() = runBlocking {
        val accounts = db.accountDao().observeActive().first()
        assertEquals(1, accounts.size)
        val efectivo = accounts.first()
        assertEquals("Efectivo", efectivo.name)
        assertEquals(AccountType.CASH, efectivo.type)
        assertEquals("GTQ", efectivo.currencyCode)
        assertEquals(0L, efectivo.initialBalanceCents)
        assertTrue(efectivo.isActive)
    }

    @Test
    fun defaultCategories_areSeededOnCreate() = runBlocking {
        val expense = db.categoryDao().observeActiveByType(TransactionType.EXPENSE).first()
        val income = db.categoryDao().observeActiveByType(TransactionType.INCOME).first()
        assertEquals(9, expense.size)
        assertEquals(5, income.size)
        assertTrue(expense.any { it.name == "Alimentación" })
        assertTrue(income.any { it.name == "Salario" })
    }

    @Test
    fun insertAndReadAccount() = runBlocking {
        val id = db.accountDao().insert(
            AccountEntity(
                name = "Banco",
                type = AccountType.BANK_ACCOUNT,
                currencyCode = "GTQ",
                initialBalanceCents = 5_000L,
                isActive = true,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
        val found = db.accountDao().findById(id)
        assertNotNull(found)
        assertEquals("Banco", found!!.name)
        assertEquals(AccountType.BANK_ACCOUNT, found.type)
    }

    @Test
    fun insertExpenseTransaction_isPersisted() = runBlocking {
        val (accountId, categoryId) = seededIds()
        db.transactionDao().insert(
            transaction(
                accountId, categoryId, TransactionType.EXPENSE, 12_575L,
                LocalDate.of(2026, 7, 19).toEpochDay(), "Compra de prueba",
            ),
        )
        val rows = db.transactionDao().observeAllWithNames().first()
        assertEquals(1, rows.size)
        assertEquals(12_575L, rows.first().amountCents)
        assertEquals(TransactionType.EXPENSE, rows.first().type)
        assertEquals("Compra de prueba", rows.first().description)
    }

    @Test
    fun insertIncomeTransaction_isPersisted() = runBlocking {
        val accountId = db.accountDao().observeActive().first().first().id
        val salario = db.categoryDao().observeActiveByType(TransactionType.INCOME).first()
            .first { it.name == "Salario" }
        db.transactionDao().insert(
            transaction(
                accountId, salario.id, TransactionType.INCOME, 500_000L,
                LocalDate.of(2026, 7, 1).toEpochDay(),
            ),
        )
        val rows = db.transactionDao().observeAllWithNames().first()
        assertEquals(1, rows.size)
        assertEquals(TransactionType.INCOME, rows.first().type)
        assertEquals(500_000L, rows.first().amountCents)
    }

    @Test
    fun monthlyQuery_returnsOnlyTransactionsInsideMonth() = runBlocking {
        val (accountId, categoryId) = seededIds()
        val days = listOf(
            LocalDate.of(2026, 6, 30),
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 31),
            LocalDate.of(2026, 8, 1),
        )
        for (day in days) {
            db.transactionDao().insert(
                transaction(accountId, categoryId, TransactionType.EXPENSE, 1_000L, day.toEpochDay()),
            )
        }
        val range = MonthRange.of(YearMonth.of(2026, 7))
        val july = db.transactionDao().observeBetween(range.startEpochDay, range.endEpochDay).first()
        assertEquals(2, july.size)
        assertTrue(july.all { it.transactionDate in range.startEpochDay..range.endEpochDay })
    }

    @Test
    fun transactions_areOrderedNewestFirst() = runBlocking {
        val (accountId, categoryId) = seededIds()
        val day10 = LocalDate.of(2026, 7, 10).toEpochDay()
        val day20 = LocalDate.of(2026, 7, 20).toEpochDay()
        val day15 = LocalDate.of(2026, 7, 15).toEpochDay()
        for (day in listOf(day10, day20, day15)) {
            db.transactionDao().insert(
                transaction(accountId, categoryId, TransactionType.EXPENSE, 1_000L, day),
            )
        }
        val rows = db.transactionDao().observeAllWithNames().first()
        assertEquals(listOf(day20, day15, day10), rows.map { it.transactionDate })
    }

    @Test
    fun joinQuery_resolvesAccountAndCategoryNames() = runBlocking {
        val (accountId, categoryId) = seededIds()
        db.transactionDao().insert(
            transaction(
                accountId, categoryId, TransactionType.EXPENSE, 12_575L,
                LocalDate.of(2026, 7, 19).toEpochDay(),
            ),
        )
        val row = db.transactionDao().observeRecentWithNames(5).first().first()
        assertEquals("Efectivo", row.accountName)
        assertEquals("Alimentación", row.categoryName)
    }
}
