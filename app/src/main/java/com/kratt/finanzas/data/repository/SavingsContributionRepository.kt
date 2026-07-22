package com.kratt.finanzas.data.repository

import androidx.room.withTransaction
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.entity.SavingsContributionEntity
import com.kratt.finanzas.data.local.entity.TransactionEntity
import com.kratt.finanzas.domain.model.ContributionType
import com.kratt.finanzas.domain.model.SavingsContribution
import com.kratt.finanzas.domain.model.SavingsGoalStatus
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.MoneyMath
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface ContributionResult {
    data class Success(val id: Long) : ContributionResult
    data object NotFound : ContributionResult
    data object InvalidAmount : ContributionResult
    data object NoLinkedAccount : ContributionResult
    data object SameAccount : ContributionResult
}

class SavingsContributionRepository(private val database: AppDatabase) {

    private val contributionDao = database.savingsContributionDao()
    private val goalDao = database.savingsGoalDao()
    private val transactionDao = database.transactionDao()

    fun observeByGoal(goalId: Long): Flow<List<SavingsContribution>> =
        contributionDao.observeByGoal(goalId).map { list -> list.map { it.toDomain() } }

    // aportes dentro de un rango de fechas, para el reporte de aportes por mes
    suspend fun listBetween(start: LocalDate, end: LocalDate): List<SavingsContribution> =
        contributionDao.listBetween(start.toEpochDay(), end.toEpochDay()).map { it.toDomain() }

    // aporte manual: solo suma al avance de la meta, no mueve dinero real
    suspend fun addManual(goalId: Long, amountCents: Long, date: LocalDate, note: String?): ContributionResult =
        database.withTransaction {
            goalDao.findById(goalId) ?: return@withTransaction ContributionResult.NotFound
            if (!validAmount(amountCents)) return@withTransaction ContributionResult.InvalidAmount
            val now = System.currentTimeMillis()
            val id = contributionDao.insert(
                SavingsContributionEntity(
                    savingsGoalId = goalId, amountCents = amountCents, contributionDate = date.toEpochDay(),
                    sourceAccountId = null, linkedTransactionId = null, contributionType = ContributionType.MANUAL_TRACKING,
                    note = note?.trim()?.ifBlank { null }, createdAt = now, updatedAt = now,
                ),
            )
            refreshGoalStatus(goalId, now)
            ContributionResult.Success(id)
        }

    // aporte por transferencia real: crea la transferencia y el aporte ligados en una sola operacion
    suspend fun addTransfer(goalId: Long, amountCents: Long, date: LocalDate, sourceAccountId: Long, note: String?): ContributionResult =
        database.withTransaction {
            val goal = goalDao.findById(goalId) ?: return@withTransaction ContributionResult.NotFound
            val dest = goal.linkedAccountId ?: return@withTransaction ContributionResult.NoLinkedAccount
            if (!validAmount(amountCents)) return@withTransaction ContributionResult.InvalidAmount
            if (sourceAccountId == dest) return@withTransaction ContributionResult.SameAccount
            val now = System.currentTimeMillis()
            // la transferencia es neutral a ingresos y gastos y afecta ambas cuentas una vez
            val txId = transactionDao.insert(
                TransactionEntity(
                    accountId = sourceAccountId, destinationAccountId = dest, categoryId = null,
                    type = TransactionType.TRANSFER, amountCents = amountCents, description = goal.name,
                    transactionDate = date.toEpochDay(), createdAt = now, updatedAt = now,
                    originKey = "savings_transfer:$goalId",
                ),
            )
            val id = contributionDao.insert(
                SavingsContributionEntity(
                    savingsGoalId = goalId, amountCents = amountCents, contributionDate = date.toEpochDay(),
                    sourceAccountId = sourceAccountId, linkedTransactionId = txId, contributionType = ContributionType.ACCOUNT_TRANSFER,
                    note = note?.trim()?.ifBlank { null }, createdAt = now, updatedAt = now,
                ),
            )
            refreshGoalStatus(goalId, now)
            ContributionResult.Success(id)
        }

    // revierte un aporte: si estaba ligado a una transferencia la borra, todo atomico
    suspend fun revert(contributionId: Long): Unit = database.withTransaction {
        val contribution = contributionDao.findById(contributionId) ?: return@withTransaction
        val txId = contribution.linkedTransactionId
        contributionDao.deleteById(contributionId)
        if (txId != null) transactionDao.findById(txId)?.let { transactionDao.delete(it) }
        refreshGoalStatus(contribution.savingsGoalId, System.currentTimeMillis())
    }

    // ajusta el estado de la meta segun el total actual de aportes
    private suspend fun refreshGoalStatus(goalId: Long, now: Long) {
        val goal = goalDao.findById(goalId) ?: return
        val total = contributionDao.totalByGoal(goalId)
        val reached = total >= goal.targetAmountCents
        if (reached && goal.status == SavingsGoalStatus.ACTIVE) {
            goalDao.setStatus(goalId, SavingsGoalStatus.COMPLETED.name, now)
        } else if (!reached && goal.status == SavingsGoalStatus.COMPLETED) {
            goalDao.setStatus(goalId, SavingsGoalStatus.ACTIVE.name, now)
        }
    }

    private fun validAmount(cents: Long): Boolean = cents > 0L && cents <= MoneyMath.MAX_SUPPORTED_CENTS
}

private fun SavingsContributionEntity.toDomain() = SavingsContribution(
    id = id, savingsGoalId = savingsGoalId, amountCents = amountCents,
    contributionDate = LocalDate.ofEpochDay(contributionDate), sourceAccountId = sourceAccountId,
    linkedTransactionId = linkedTransactionId, contributionType = contributionType, note = note,
    createdAt = createdAt, updatedAt = updatedAt,
)
