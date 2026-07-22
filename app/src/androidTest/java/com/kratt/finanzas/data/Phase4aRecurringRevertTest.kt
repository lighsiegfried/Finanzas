package com.kratt.finanzas.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.DefaultDataCallback
import com.kratt.finanzas.data.repository.AccountRepositoryImpl
import com.kratt.finanzas.data.repository.CategoryRepositoryImpl
import com.kratt.finanzas.data.repository.RecurringRepository
import com.kratt.finanzas.data.repository.RevertResult
import com.kratt.finanzas.data.repository.TransactionRepositoryImpl
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.PostingMode
import com.kratt.finanzas.domain.model.RecurrenceType
import com.kratt.finanzas.domain.model.RecurringOccurrenceStatus
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.DeleteTransactionUseCase
import com.kratt.finanzas.domain.usecase.ReportAggregator
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// pruebas de integracion del reverso recurrente y del bloqueo de borrado de movimientos generados
@RunWith(AndroidJUnit4::class)
class Phase4aRecurringRevertTest {

    private lateinit var db: AppDatabase
    private lateinit var accounts: AccountRepositoryImpl
    private lateinit var categories: CategoryRepositoryImpl
    private lateinit var transactions: TransactionRepositoryImpl
    private lateinit var recurring: RecurringRepository
    private lateinit var deleteTransaction: DeleteTransactionUseCase

    private val today = LocalDate.of(2026, 7, 15)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(DefaultDataCallback())
            .build()
        accounts = AccountRepositoryImpl(db.accountDao()) { 1L }
        categories = CategoryRepositoryImpl(db.categoryDao()) { 1L }
        transactions = TransactionRepositoryImpl(db.transactionDao())
        recurring = RecurringRepository(db) { 1L }
        deleteTransaction = DeleteTransactionUseCase(transactions)
    }

    @After
    fun tearDown() = db.close()

    // crea una plantilla de gasto recurrente y devuelve el id de la plantilla y de su primera ocurrencia
    private fun seedTemplateWithOccurrence(): Pair<Long, Long> = runBlocking {
        val account = accounts.insert(Account(0L, "Efectivo", AccountType.CASH, "GTQ", 500_000, true))
        val category = categories.insert(Category(0L, "Servicios", TransactionType.EXPENSE, "home", false, true))
        val templateId = recurring.createTemplate(
            name = "Internet", transactionType = TransactionType.EXPENSE, accountId = account, categoryId = category,
            amountCents = 20_000, recurrenceType = RecurrenceType.MONTHLY, interval = 1, startDate = today,
            endDate = null, postingMode = PostingMode.REQUIRE_CONFIRMATION, description = null, today = today,
        )
        val occurrence = recurring.nextPending(templateId)!!
        templateId to occurrence.id
    }

    private suspend fun expenseTotal(): Long =
        ReportAggregator.incomeExpense(transactions.observeAllTransactions().first()).expenseCents

    @Test
    fun revert_removesGeneratedMovement_andReopensOccurrence() = runBlocking {
        val (templateId, occurrenceId) = seedTemplateWithOccurrence()
        recurring.postOccurrence(occurrenceId)

        // al registrar aparece el gasto real y la ocurrencia queda POSTED con su movimiento
        assertEquals(20_000L, expenseTotal())
        val posted = recurring.observeOccurrences(templateId).first().first { it.id == occurrenceId }
        assertEquals(RecurringOccurrenceStatus.POSTED, posted.status)
        assertNotNull(posted.generatedTransactionId)

        val result = recurring.revertOccurrence(occurrenceId)
        assertEquals(RevertResult.Success, result)

        // el movimiento generado desaparece y la ocurrencia vuelve a pendiente
        assertEquals(0L, expenseTotal())
        val reverted = recurring.observeOccurrences(templateId).first().first { it.id == occurrenceId }
        assertEquals(RecurringOccurrenceStatus.PENDING, reverted.status)
        assertNull(reverted.generatedTransactionId)
        assertNull(transactions.findById(posted.generatedTransactionId!!))
    }

    @Test
    fun revert_neverPosted_returnsNotPosted() = runBlocking {
        val (_, occurrenceId) = seedTemplateWithOccurrence()
        assertEquals(RevertResult.NotPosted, recurring.revertOccurrence(occurrenceId))
    }

    @Test
    fun revert_alreadyReverted_returnsNotPosted() = runBlocking {
        val (_, occurrenceId) = seedTemplateWithOccurrence()
        recurring.postOccurrence(occurrenceId)
        assertEquals(RevertResult.Success, recurring.revertOccurrence(occurrenceId))
        // un segundo reverso no encuentra nada que revertir
        assertEquals(RevertResult.NotPosted, recurring.revertOccurrence(occurrenceId))
    }

    @Test
    fun revert_mismatchedOriginKey_returnsMismatch_andKeepsData() = runBlocking {
        val (templateId, occurrenceId) = seedTemplateWithOccurrence()
        recurring.postOccurrence(occurrenceId)
        val posted = recurring.observeOccurrences(templateId).first().first { it.id == occurrenceId }
        val generatedId = posted.generatedTransactionId!!

        // se corrompe la marca de origen para simular un vinculo que no corresponde
        val generated = transactions.findById(generatedId)!!
        transactions.update(generated.copy(originKey = "recurring:999999"))

        assertEquals(RevertResult.Mismatch, recurring.revertOccurrence(occurrenceId))
        // ni el movimiento ni la ocurrencia cambian ante un vinculo que no coincide
        assertNotNull(transactions.findById(generatedId))
        val still = recurring.observeOccurrences(templateId).first().first { it.id == occurrenceId }
        assertEquals(RecurringOccurrenceStatus.POSTED, still.status)
    }

    @Test
    fun deleteUseCase_blocksGeneratedMovement_butAllowsNormalOne() = runBlocking {
        val (templateId, occurrenceId) = seedTemplateWithOccurrence()
        recurring.postOccurrence(occurrenceId)
        val generatedId = recurring.observeOccurrences(templateId).first()
            .first { it.id == occurrenceId }.generatedTransactionId!!
        val generated = transactions.findById(generatedId)!!

        // un movimiento generado no se borra por el flujo normal
        assertFalse(deleteTransaction(generated))
        assertNotNull(transactions.findById(generatedId))

        // un movimiento normal si se puede borrar
        val account = accounts.observeAllAccounts().first().first().id
        val normalId = transactions.insert(
            Transaction(0L, account, TransactionType.EXPENSE, 5_000, null, today, 1L, 1L, categoryId = null),
        )
        assertTrue(deleteTransaction(transactions.findById(normalId)!!))
        assertNull(transactions.findById(normalId))
    }
}
