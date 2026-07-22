package com.kratt.finanzas.data.repository

import androidx.room.withTransaction
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.entity.InstallmentOccurrenceEntity
import com.kratt.finanzas.data.local.entity.InstallmentPlanEntity
import com.kratt.finanzas.data.local.entity.TransactionEntity
import com.kratt.finanzas.domain.model.InstallmentFrequency
import com.kratt.finanzas.domain.model.InstallmentOccurrence
import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus
import com.kratt.finanzas.domain.model.InstallmentPlan
import com.kratt.finanzas.domain.model.InstallmentStatus
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.InstallmentPaymentRules
import com.kratt.finanzas.domain.usecase.InstallmentScheduleGenerator
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface PaymentResult {
    data object Success : PaymentResult
    data object AlreadyPaid : PaymentResult
    data object NotFound : PaymentResult
}

// administra compras en cuotas con contabilidad de flujo de caja
class InstallmentRepository(
    private val database: AppDatabase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val installmentDao = database.installmentDao()
    private val transactionDao = database.transactionDao()

    fun observePlans(): Flow<List<InstallmentPlan>> =
        installmentDao.observePlans().map { list -> list.map { it.toDomain() } }

    suspend fun findPlan(id: Long): InstallmentPlan? = installmentDao.findPlan(id)?.toDomain()

    fun observeOccurrences(planId: Long): Flow<List<InstallmentOccurrence>> =
        installmentDao.observeOccurrences(planId).map { list -> list.map { it.toDomain() } }

    suspend fun occurrencesForPlan(planId: Long): List<InstallmentOccurrence> =
        installmentDao.occurrencesForPlan(planId).map { it.toDomain() }

    // crea el plan y genera todas las cuotas pendientes, sin registrar el gasto completo
    suspend fun createPlan(
        name: String,
        accountId: Long,
        categoryId: Long,
        totalCents: Long,
        installmentCount: Int,
        firstDueDate: LocalDate,
        description: String?,
    ): Long = database.withTransaction {
        val now = nowMillis()
        val schedule = InstallmentScheduleGenerator.generate(firstDueDate, installmentCount, totalCents)
        val planId = installmentDao.insertPlan(
            InstallmentPlanEntity(
                name = name.trim(), accountId = accountId, categoryId = categoryId, totalAmountCents = totalCents,
                installmentCount = installmentCount, installmentAmountCents = schedule.first().amountCents,
                firstDueDate = firstDueDate.toEpochDay(), frequency = InstallmentFrequency.MONTHLY,
                paidInstallments = 0, status = InstallmentStatus.ACTIVE,
                description = description?.trim()?.takeIf { it.isNotEmpty() }, createdAt = now, updatedAt = now,
            ),
        )
        installmentDao.insertOccurrences(
            schedule.map { item ->
                InstallmentOccurrenceEntity(
                    installmentPlanId = planId, sequenceNumber = item.sequenceNumber, dueDate = item.dueDate.toEpochDay(),
                    amountCents = item.amountCents, status = InstallmentOccurrenceStatus.PENDING,
                    createdAt = now, updatedAt = now,
                )
            },
        )
        planId
    }

    suspend fun setStatus(planId: Long, status: InstallmentStatus) {
        val plan = installmentDao.findPlan(planId) ?: return
        installmentDao.updatePlan(plan.copy(status = status, updatedAt = nowMillis()))
    }

    // registra el pago de una cuota como un gasto real, de forma atomica y sin duplicar
    suspend fun payOccurrence(occurrenceId: Long): PaymentResult = database.withTransaction {
        val occurrence = installmentDao.findOccurrence(occurrenceId) ?: return@withTransaction PaymentResult.NotFound
        if (!InstallmentPaymentRules.canPay(occurrence.status, occurrence.paidTransactionId)) {
            return@withTransaction PaymentResult.AlreadyPaid
        }
        val plan = installmentDao.findPlan(occurrence.installmentPlanId) ?: return@withTransaction PaymentResult.NotFound
        val now = nowMillis()
        val transactionId = transactionDao.insert(
            TransactionEntity(
                accountId = plan.accountId, categoryId = plan.categoryId, type = TransactionType.EXPENSE,
                amountCents = occurrence.amountCents, description = plan.name, transactionDate = occurrence.dueDate,
                createdAt = now, updatedAt = now, originKey = "installment:${occurrence.id}",
            ),
        )
        installmentDao.updateOccurrence(
            occurrence.copy(
                status = InstallmentOccurrenceStatus.PAID, paidTransactionId = transactionId, paidAt = now, updatedAt = now,
            ),
        )
        val paidCount = installmentDao.countPaid(plan.id)
        val newStatus = if (paidCount >= plan.installmentCount) InstallmentStatus.COMPLETED else plan.status
        installmentDao.updatePlan(plan.copy(paidInstallments = paidCount, status = newStatus, updatedAt = now))
        PaymentResult.Success
    }

    // revierte un pago: desvincula la cuota y luego borra el movimiento, todo atomico
    suspend fun revertOccurrence(occurrenceId: Long) = database.withTransaction {
        val occurrence = installmentDao.findOccurrence(occurrenceId) ?: return@withTransaction
        if (!InstallmentPaymentRules.canRevert(occurrence.status, occurrence.paidTransactionId)) return@withTransaction
        val transactionId = occurrence.paidTransactionId!!
        val now = nowMillis()
        installmentDao.updateOccurrence(
            occurrence.copy(status = InstallmentOccurrenceStatus.PENDING, paidTransactionId = null, paidAt = null, updatedAt = now),
        )
        transactionDao.findById(transactionId)?.let { transactionDao.delete(it) }
        val plan = installmentDao.findPlan(occurrence.installmentPlanId) ?: return@withTransaction
        val paidCount = installmentDao.countPaid(plan.id)
        val reopened = if (plan.status == InstallmentStatus.COMPLETED) InstallmentStatus.ACTIVE else plan.status
        installmentDao.updatePlan(plan.copy(paidInstallments = paidCount, status = reopened, updatedAt = now))
    }

    // marca vencidas las cuotas pendientes con fecha pasada
    suspend fun refreshOverdue(today: LocalDate) {
        installmentDao.markOverdue(today.toEpochDay(), nowMillis())
    }

    suspend fun occurrencesDueBetween(start: LocalDate, end: LocalDate): List<InstallmentOccurrence> =
        installmentDao.occurrencesDueBetween(start.toEpochDay(), end.toEpochDay()).map { it.toDomain() }
}
