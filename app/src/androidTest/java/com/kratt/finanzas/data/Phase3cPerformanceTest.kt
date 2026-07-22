package com.kratt.finanzas.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.DefaultDataCallback
import com.kratt.finanzas.data.repository.AccountRepositoryImpl
import com.kratt.finanzas.data.repository.CategoryRepositoryImpl
import com.kratt.finanzas.data.repository.ReportRepository
import com.kratt.finanzas.data.repository.TransactionRepositoryImpl
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.DateRange
import java.time.LocalDate
import java.time.YearMonth
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// mide el tiempo de los reportes con 10000 movimientos, todo por agregacion sql
@RunWith(AndroidJUnit4::class)
class Phase3cPerformanceTest {

    private lateinit var db: AppDatabase
    private lateinit var accounts: AccountRepositoryImpl
    private lateinit var categories: CategoryRepositoryImpl
    private lateinit var transactions: TransactionRepositoryImpl
    private lateinit var reports: ReportRepository

    private val tag = "Phase3cPerf"
    private val yearRange = DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(DefaultDataCallback())
            .build()
        accounts = AccountRepositoryImpl(db.accountDao()) { 1L }
        categories = CategoryRepositoryImpl(db.categoryDao()) { 1L }
        transactions = TransactionRepositoryImpl(db.transactionDao())
        reports = ReportRepository(db, accounts)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun reports_stayFastWith10kTransactions() = runBlocking {
        val accountIds = (1..5).map { accounts.insert(Account(0L, "Cuenta $it", AccountType.CASH, "GTQ", 100_000, true)) }
        val categoryIds = (1..8).map { categories.insert(Category(0L, "Categoria $it", TransactionType.EXPENSE, "other", false, true)) }
        val incomeCat = categories.insert(Category(0L, "Salario", TransactionType.INCOME, "work", false, true))

        val insertMillis = measureTimeMillis {
            db.withTransaction {
                for (i in 0 until 10_000) {
                    val account = accountIds[i % accountIds.size]
                    val monthValue = (i % 12) + 1
                    val day = (i % 27) + 1
                    val isIncome = i % 20 == 0
                    val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
                    val category = if (isIncome) incomeCat else categoryIds[i % categoryIds.size]
                    transactions.insert(
                        Transaction(0L, account, type, 1_000L + (i % 500) * 10L, null, LocalDate.of(2026, monthValue, day), 1L, 1L, categoryId = category),
                    )
                }
            }
        }
        Log.i(tag, "insert 10000 movimientos: ${insertMillis}ms")

        val incomeExpenseMs = measureTimeMillis { reports.incomeExpense(yearRange) }
        val byCategoryMs = measureTimeMillis { reports.expensesByCategory(yearRange) }
        val byAccountMs = measureTimeMillis { reports.expensesByAccount(yearRange) }
        val trendMs = measureTimeMillis { reports.monthlyTrend(YearMonth.of(2026, 12), 12) }
        val accountReportMs = measureTimeMillis { reports.accountReport(yearRange) }

        Log.i(tag, "incomeExpense: ${incomeExpenseMs}ms")
        Log.i(tag, "expensesByCategory: ${byCategoryMs}ms")
        Log.i(tag, "expensesByAccount: ${byAccountMs}ms")
        Log.i(tag, "monthlyTrend(12): ${trendMs}ms")
        Log.i(tag, "accountReport: ${accountReportMs}ms")

        // cada reporte agregado debe resolverse rapido incluso con historial grande
        assertTrue("incomeExpense lento: ${incomeExpenseMs}ms", incomeExpenseMs < 2_000)
        assertTrue("expensesByCategory lento: ${byCategoryMs}ms", byCategoryMs < 2_000)
        assertTrue("expensesByAccount lento: ${byAccountMs}ms", byAccountMs < 2_000)
        assertTrue("monthlyTrend lento: ${trendMs}ms", trendMs < 3_000)
        assertTrue("accountReport lento: ${accountReportMs}ms", accountReportMs < 3_000)
    }
}
