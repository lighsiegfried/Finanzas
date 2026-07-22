package com.kratt.finanzas.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.DefaultDataCallback
import com.kratt.finanzas.data.repository.PostResult
import com.kratt.finanzas.data.repository.RecurringRepository
import com.kratt.finanzas.domain.model.PostingMode
import com.kratt.finanzas.domain.model.RecurrenceType
import com.kratt.finanzas.domain.model.RecurringOccurrenceStatus
import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// pruebas del motor de recurrentes sobre una base en memoria con el esquema real
@RunWith(AndroidJUnit4::class)
class Phase3bRecurringTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: RecurringRepository
    private var accountId = 0L
    private var categoryId = 0L
    private val today = LocalDate.of(2026, 7, 20)

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(DefaultDataCallback())
            .build()
        repo = RecurringRepository(db) { 1L }
        accountId = db.accountDao().observeActive().first().first().id
        categoryId = db.categoryDao().observeActiveByType(TransactionType.EXPENSE).first().first().id
    }

    @After
    fun tearDown() = db.close()

    private suspend fun recurringExpenseCount(): Int =
        db.transactionDao().observeAll().first().count { it.originKey?.startsWith("recurring:") == true }

    private suspend fun createInternet(mode: PostingMode, start: LocalDate): Long = repo.createTemplate(
        name = "Internet", transactionType = TransactionType.EXPENSE, accountId = accountId, categoryId = categoryId,
        amountCents = 20_000, recurrenceType = RecurrenceType.MONTHLY, interval = 1, startDate = start,
        endDate = null, postingMode = mode, description = null, today = today,
    )

    @Test
    fun createTemplate_generatesBoundedAndIdempotent() = runBlocking {
        val id = createInternet(PostingMode.REQUIRE_CONFIRMATION, today)
        val firstCount = repo.observeOccurrences(id).first().size
        assertTrue(firstCount > 0)
        // volver a generar no duplica
        repo.generateDueOccurrences(today)
        assertEquals(firstCount, repo.observeOccurrences(id).first().size)
    }

    @Test
    fun confirmOccurrence_createsTransactionOnce() = runBlocking {
        createInternet(PostingMode.REQUIRE_CONFIRMATION, today)
        val next = repo.nextPending(db.recurringDao().activeTemplates().first().id)!!
        assertEquals(PostResult.Success, repo.postOccurrence(next.id))
        assertEquals(1, recurringExpenseCount())
        assertEquals(PostResult.AlreadyPosted, repo.postOccurrence(next.id))
        assertEquals(1, recurringExpenseCount())
    }

    @Test
    fun skipOccurrence_keepsTemplateActive() = runBlocking {
        val id = createInternet(PostingMode.REQUIRE_CONFIRMATION, today)
        val next = repo.nextPending(id)!!
        repo.skipOccurrence(next.id)
        assertEquals(RecurringOccurrenceStatus.SKIPPED, repo.observeOccurrences(id).first().first { it.id == next.id }.status)
        assertTrue(db.recurringDao().activeTemplates().any { it.id == id })
    }

    @Test
    fun autoPost_registersDueOccurrencesExactlyOnce() = runBlocking {
        createInternet(PostingMode.AUTO_POST, today.minusMonths(1))
        repo.autoPostDue(today)
        val afterFirst = recurringExpenseCount()
        assertTrue(afterFirst >= 1)
        // segunda corrida no vuelve a registrar
        repo.autoPostDue(today)
        assertEquals(afterFirst, recurringExpenseCount())
    }
}
