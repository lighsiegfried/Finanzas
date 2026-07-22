package com.kratt.finanzas.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.DefaultDataCallback
import com.kratt.finanzas.data.report.CsvCell
import com.kratt.finanzas.data.report.CsvExporter
import com.kratt.finanzas.data.report.CsvTable
import com.kratt.finanzas.data.repository.AccountRepositoryImpl
import com.kratt.finanzas.data.repository.BudgetRepository
import com.kratt.finanzas.data.repository.CategoryRepositoryImpl
import com.kratt.finanzas.data.repository.ReportRepository
import com.kratt.finanzas.data.repository.TransactionRepositoryImpl
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.BudgetCalculator
import com.kratt.finanzas.domain.usecase.BudgetSpentCalculator
import com.kratt.finanzas.domain.usecase.BudgetState
import com.kratt.finanzas.domain.usecase.DateRange
import java.io.ByteArrayOutputStream
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

// pruebas de integracion de la fase 3c: presupuestos, agregaciones de reportes y csv
@RunWith(AndroidJUnit4::class)
class Phase3cReportsTest {

    private lateinit var db: AppDatabase
    private lateinit var accounts: AccountRepositoryImpl
    private lateinit var categories: CategoryRepositoryImpl
    private lateinit var transactions: TransactionRepositoryImpl
    private lateinit var budgets: BudgetRepository
    private lateinit var reports: ReportRepository

    private val month = YearMonth.of(2026, 7)
    private val range = DateRange(month.atDay(1), month.atEndOfMonth())

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(DefaultDataCallback())
            .build()
        accounts = AccountRepositoryImpl(db.accountDao()) { 1L }
        categories = CategoryRepositoryImpl(db.categoryDao()) { 1L }
        transactions = TransactionRepositoryImpl(db.transactionDao())
        budgets = BudgetRepository(db) { 1L }
        reports = ReportRepository(db, accounts)
    }

    @After
    fun tearDown() = db.close()

    private fun expenseCategory(name: String): Long = runBlocking {
        categories.insert(Category(0L, name, TransactionType.EXPENSE, "other", false, true))
    }

    private fun incomeCategory(name: String): Long = runBlocking {
        categories.insert(Category(0L, name, TransactionType.INCOME, "work", false, true))
    }

    private fun expense(accountId: Long, categoryId: Long, cents: Long, day: Int) = runBlocking {
        transactions.insert(Transaction(0L, accountId, TransactionType.EXPENSE, cents, null, LocalDate.of(2026, 7, day), 1L, 1L, categoryId = categoryId))
    }

    private fun income(accountId: Long, categoryId: Long, cents: Long, day: Int) = runBlocking {
        transactions.insert(Transaction(0L, accountId, TransactionType.INCOME, cents, null, LocalDate.of(2026, 7, day), 1L, 1L, categoryId = categoryId))
    }

    // arma el escenario canonico de la fase 3c y devuelve los ids de las categorias
    private fun seedScenario(): Map<String, Long> {
        val cash = runBlocking { accounts.insert(Account(0L, "Efectivo", AccountType.CASH, "GTQ", 0L, true)) }
        val salario = incomeCategory("Salario")
        val alimentacion = expenseCategory("Alimentacion")
        val transporte = expenseCategory("Transporte")
        val hogar = expenseCategory("Hogar")
        val servicios = expenseCategory("Servicios")
        income(cash, salario, 800_000, 1)
        expense(cash, alimentacion, 85_000, 5)
        expense(cash, transporte, 30_000, 6)
        expense(cash, hogar, 310_000, 7)
        expense(cash, servicios, 100_000, 8)
        return mapOf("cash" to cash, "alimentacion" to alimentacion, "hogar" to hogar, "transporte" to transporte, "servicios" to servicios)
    }

    @Test
    fun incomeExpense_matchesCanonicalScenario() = runBlocking {
        seedScenario()
        val ie = reports.incomeExpense(range)
        assertEquals(800_000L, ie.incomeCents)
        assertEquals(525_000L, ie.expenseCents)
        assertEquals(275_000L, ie.netCents)
    }

    @Test
    fun expensesByCategory_areAggregatedAndOrderedDescending() = runBlocking {
        val ids = seedScenario()
        val rows = reports.expensesByCategory(range)
        // la categoria mayor va primero
        assertEquals(ids["hogar"], rows.first().id)
        assertEquals(310_000L, rows.first { it.id == ids["hogar"] }.totalCents)
        assertEquals(85_000L, rows.first { it.id == ids["alimentacion"] }.totalCents)
        assertEquals(525_000L, rows.sumOf { it.totalCents })
    }

    @Test
    fun budgetProgress_overallWarning_andCategoryAvailable() = runBlocking {
        val ids = seedScenario()
        budgets.createBudget(month.year, month.monthValue, null, 600_000, 80)
        budgets.createBudget(month.year, month.monthValue, ids["alimentacion"], 120_000, 80)

        val monthly = transactions.observeMonthly(month).first()

        val overallSpent = BudgetSpentCalculator.spent(monthly, null)
        val overall = BudgetCalculator.progress(600_000, overallSpent, 80)
        assertEquals(525_000L, overallSpent)
        assertEquals(75_000L, overall.remainingCents)
        assertEquals(87, overall.percentage)
        assertEquals(BudgetState.WARNING, overall.state)

        val foodSpent = BudgetSpentCalculator.spent(monthly, ids["alimentacion"])
        val food = BudgetCalculator.progress(120_000, foodSpent, 80)
        assertEquals(85_000L, foodSpent)
        assertEquals(35_000L, food.remainingCents)
        assertEquals(BudgetState.AVAILABLE, food.state)
    }

    @Test
    fun budgetExceeded_whenSpentOverLimit() = runBlocking {
        val ids = seedScenario()
        budgets.createBudget(month.year, month.monthValue, ids["hogar"], 200_000, 80)
        val monthly = transactions.observeMonthly(month).first()
        val spent = BudgetSpentCalculator.spent(monthly, ids["hogar"])
        val progress = BudgetCalculator.progress(200_000, spent, 80)
        assertEquals(BudgetState.EXCEEDED, progress.state)
        assertTrue(progress.remainingCents < 0)
    }

    @Test
    fun budgetCrud_observeOrderingAndExists() = runBlocking {
        val overallId = budgets.createBudget(month.year, month.monthValue, null, 600_000, 80)
        val food = expenseCategory("Alimentacion")
        budgets.createBudget(month.year, month.monthValue, food, 120_000, 90)

        val list = budgets.observeForMonth(month.year, month.monthValue).first()
        assertEquals(2, list.size)
        // el general va primero
        assertTrue(list.first().isOverall)
        assertTrue(budgets.exists(month.year, month.monthValue, null))
        assertTrue(budgets.exists(month.year, month.monthValue, food))
        assertFalse(budgets.exists(month.year, month.monthValue, 99_999L))

        val loaded = budgets.findById(overallId)!!
        budgets.updateBudget(loaded.copy(limitAmountCents = 700_000, warningPercentage = 75))
        assertEquals(700_000L, budgets.findById(overallId)!!.limitAmountCents)

        budgets.deleteBudget(overallId)
        assertEquals(1, budgets.observeForMonth(month.year, month.monthValue).first().size)
    }

    @Test
    fun creditCardDebt_reportsOutstandingBalance() = runBlocking {
        val card = accounts.insert(Account(0L, "Visa", AccountType.CREDIT_CARD, "GTQ", 0L, true, creditLimitCents = 500_000))
        val cat = expenseCategory("Compras")
        expense(card, cat, 120_000, 3)
        val debt = reports.creditCardDebt()
        val visa = debt.first { it.first == "Visa" }
        assertEquals(120_000L, visa.second.debtCents)
        assertEquals(380_000L, visa.second.availableCreditCents)
    }

    @Test
    fun accountReport_hasOpeningAndClosingBalances() = runBlocking {
        val ids = seedScenario()
        val rows = reports.accountReport(range)
        val cash = rows.first { it.accountId == ids["cash"] }
        assertEquals(0L, cash.openingCents)
        // cierre = 0 + 800000 ingreso - 525000 gasto
        assertEquals(275_000L, cash.closingCents)
    }

    @Test
    fun monthlyTrend_returnsRequestedNumberOfPoints() = runBlocking {
        seedScenario()
        val points = reports.monthlyTrend(month, 6)
        assertEquals(6, points.size)
        val july = points.first { it.year == 2026 && it.month == 7 }
        assertEquals(275_000L, july.balanceCents)
    }

    @Test
    fun csvExport_escapesAndProtectsAgainstFormulaInjection() {
        val table = CsvTable(
            header = listOf("Categoria", "Total"),
            rows = listOf(
                listOf(CsvCell.Text("=SUM(A1)"), CsvCell.Value("85000")),
                listOf(CsvCell.Text("Café, con coma"), CsvCell.Value("30000")),
            ),
        )
        val output = ByteArrayOutputStream()
        CsvExporter().write(output, table)
        val text = output.toString("UTF-8")

        // el bom va al inicio para que la hoja de calculo lea acentos
        assertTrue(text.startsWith("﻿"))
        // la formula se neutraliza con un apostrofo al frente
        assertTrue(text.contains("'=SUM(A1)"))
        // el texto con coma va entre comillas
        assertTrue(text.contains("\"Café, con coma\""))
        // los valores numericos no se alteran
        assertTrue(text.contains("85000"))
    }
}
