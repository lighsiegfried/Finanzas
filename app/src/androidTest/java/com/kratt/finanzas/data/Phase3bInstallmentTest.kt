package com.kratt.finanzas.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.DefaultDataCallback
import com.kratt.finanzas.data.repository.InstallmentRepository
import com.kratt.finanzas.data.repository.PaymentResult
import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus
import com.kratt.finanzas.domain.model.InstallmentStatus
import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// pruebas del motor de cuotas sobre una base en memoria con el esquema real
@RunWith(AndroidJUnit4::class)
class Phase3bInstallmentTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: InstallmentRepository
    private var accountId = 0L
    private var categoryId = 0L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(DefaultDataCallback())
            .build()
        repo = InstallmentRepository(db) { 1L }
        accountId = db.accountDao().observeActive().first().first().id
        categoryId = db.categoryDao().observeActiveByType(TransactionType.EXPENSE).first().first().id
    }

    @After
    fun tearDown() = db.close()

    private suspend fun expenseCountWithOrigin(): Int =
        db.transactionDao().observeAll().first().count { it.originKey?.startsWith("installment:") == true }

    @Test
    fun createPlan_generatesExactOccurrences() = runBlocking {
        val planId = repo.createPlan("Monitor", accountId, categoryId, 153_600, 12, LocalDate.of(2026, 7, 20), null)
        val occurrences = repo.occurrencesForPlan(planId)
        assertEquals(12, occurrences.size)
        assertEquals(153_600L, occurrences.sumOf { it.amountCents })
        assertTrue(occurrences.all { it.status == InstallmentOccurrenceStatus.PENDING })
        assertEquals(1, occurrences.first().sequenceNumber)
    }

    @Test
    fun payOccurrence_createsOneExpense_andPreventsDuplicate() = runBlocking {
        val planId = repo.createPlan("Monitor", accountId, categoryId, 153_600, 12, LocalDate.of(2026, 7, 20), null)
        val first = repo.occurrencesForPlan(planId).first()

        assertEquals(PaymentResult.Success, repo.payOccurrence(first.id))
        assertEquals(1, expenseCountWithOrigin())
        assertEquals(InstallmentOccurrenceStatus.PAID, repo.occurrencesForPlan(planId).first().status)
        assertEquals(1, repo.findPlan(planId)!!.paidInstallments)

        // un segundo intento no crea otro gasto
        assertEquals(PaymentResult.AlreadyPaid, repo.payOccurrence(first.id))
        assertEquals(1, expenseCountWithOrigin())
    }

    @Test
    fun payingAllOccurrences_completesPlan() = runBlocking {
        val planId = repo.createPlan("Monitor", accountId, categoryId, 6_000, 3, LocalDate.of(2026, 7, 20), null)
        repo.occurrencesForPlan(planId).forEach { repo.payOccurrence(it.id) }
        assertEquals(InstallmentStatus.COMPLETED, repo.findPlan(planId)!!.status)
        assertEquals(3, expenseCountWithOrigin())
    }

    @Test
    fun revertPayment_removesTransaction_andReopensPlan() = runBlocking {
        val planId = repo.createPlan("Monitor", accountId, categoryId, 6_000, 3, LocalDate.of(2026, 7, 20), null)
        val occurrences = repo.occurrencesForPlan(planId)
        occurrences.forEach { repo.payOccurrence(it.id) }
        assertEquals(InstallmentStatus.COMPLETED, repo.findPlan(planId)!!.status)

        val paidFirst = repo.occurrencesForPlan(planId).first()
        repo.revertOccurrence(paidFirst.id)
        val reverted = repo.occurrencesForPlan(planId).first()
        assertEquals(InstallmentOccurrenceStatus.PENDING, reverted.status)
        assertNull(reverted.paidTransactionId)
        assertEquals(2, repo.findPlan(planId)!!.paidInstallments)
        assertEquals(InstallmentStatus.ACTIVE, repo.findPlan(planId)!!.status)
        assertEquals(2, expenseCountWithOrigin())
    }

    @Test
    fun pauseAndResume() = runBlocking {
        val planId = repo.createPlan("Monitor", accountId, categoryId, 6_000, 3, LocalDate.of(2026, 7, 20), null)
        repo.setStatus(planId, InstallmentStatus.PAUSED)
        assertEquals(InstallmentStatus.PAUSED, repo.findPlan(planId)!!.status)
        repo.setStatus(planId, InstallmentStatus.ACTIVE)
        assertEquals(InstallmentStatus.ACTIVE, repo.findPlan(planId)!!.status)
    }
}
