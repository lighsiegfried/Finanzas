package com.kratt.finanzas.data.repository

import androidx.room.withTransaction
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.entity.PlannedPurchaseEntity
import com.kratt.finanzas.data.local.entity.TransactionEntity
import com.kratt.finanzas.domain.model.PlannedPurchase
import com.kratt.finanzas.domain.model.PurchaseStatus
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.MoneyMath
import com.kratt.finanzas.domain.usecase.PurchaseValidationError
import com.kratt.finanzas.domain.usecase.PlannedPurchaseValidator
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface PurchaseSaveResult {
    data class Success(val id: Long) : PurchaseSaveResult
    data class Invalid(val errors: Set<PurchaseValidationError>) : PurchaseSaveResult
    // otra compra activa ya esta ligada a esa meta
    data object GoalAlreadyLinked : PurchaseSaveResult
}

sealed interface RegisterPurchaseResult {
    data object Success : RegisterPurchaseResult
    data object NotFound : RegisterPurchaseResult
    data object AlreadyPurchased : RegisterPurchaseResult
    data object InvalidAmount : RegisterPurchaseResult
}

class PlannedPurchaseRepository(private val database: AppDatabase) {

    private val purchaseDao = database.plannedPurchaseDao()
    private val transactionDao = database.transactionDao()

    fun observeAll(): Flow<List<PlannedPurchase>> = purchaseDao.observeAll().map { list -> list.map { it.toDomain() } }
    fun observeById(id: Long): Flow<PlannedPurchase?> = purchaseDao.observeById(id).map { it?.toDomain() }
    suspend fun findById(id: Long): PlannedPurchase? = purchaseDao.findById(id)?.toDomain()

    // valida y aplica la regla de una meta con una sola compra activa
    suspend fun save(purchase: PlannedPurchase): PurchaseSaveResult = database.withTransaction {
        val trimmed = purchase.copy(name = purchase.name.trim(), description = purchase.description?.trim()?.ifBlank { null })
        val errors = PlannedPurchaseValidator.validate(trimmed)
        if (errors.isNotEmpty()) return@withTransaction PurchaseSaveResult.Invalid(errors)
        val goalId = trimmed.savingsGoalId
        if (goalId != null) {
            val conflict = purchaseDao.activeByGoal(goalId).any { it.id != trimmed.id }
            if (conflict) return@withTransaction PurchaseSaveResult.GoalAlreadyLinked
        }
        val now = System.currentTimeMillis()
        if (trimmed.id == 0L) {
            PurchaseSaveResult.Success(purchaseDao.insert(trimmed.toEntity(createdAt = now, updatedAt = now)))
        } else {
            val existing = purchaseDao.findById(trimmed.id) ?: return@withTransaction PurchaseSaveResult.Invalid(emptySet())
            purchaseDao.update(trimmed.toEntity(createdAt = existing.createdAt, updatedAt = now, purchasedTransactionId = existing.purchasedTransactionId))
            PurchaseSaveResult.Success(trimmed.id)
        }
    }

    suspend fun setStatus(id: Long, status: PurchaseStatus) = purchaseDao.setStatus(id, status.name, System.currentTimeMillis())

    // registra la compra: crea un unico gasto real ligado y marca comprada; no duplica
    suspend fun registerPurchase(
        purchaseId: Long,
        finalAmountCents: Long,
        accountId: Long,
        categoryId: Long?,
        date: LocalDate,
        description: String?,
    ): RegisterPurchaseResult = database.withTransaction {
        val purchase = purchaseDao.findById(purchaseId) ?: return@withTransaction RegisterPurchaseResult.NotFound
        // evita registrar dos veces la misma compra
        if (purchase.status == PurchaseStatus.PURCHASED || purchase.purchasedTransactionId != null) {
            return@withTransaction RegisterPurchaseResult.AlreadyPurchased
        }
        if (finalAmountCents <= 0L || finalAmountCents > MoneyMath.MAX_SUPPORTED_CENTS) {
            return@withTransaction RegisterPurchaseResult.InvalidAmount
        }
        val now = System.currentTimeMillis()
        val txId = transactionDao.insert(
            TransactionEntity(
                accountId = accountId, categoryId = categoryId, type = TransactionType.EXPENSE,
                amountCents = finalAmountCents, description = description?.trim()?.ifBlank { null } ?: purchase.name,
                transactionDate = date.toEpochDay(), createdAt = now, updatedAt = now,
                originKey = "planned_purchase:$purchaseId",
            ),
        )
        purchaseDao.update(purchase.copy(status = PurchaseStatus.PURCHASED, purchasedTransactionId = txId, updatedAt = now))
        RegisterPurchaseResult.Success
    }

    // revierte la compra: borra el gasto generado y vuelve a pendiente; verifica el origen y no duplica
    suspend fun reversePurchase(purchaseId: Long): Unit = database.withTransaction {
        val purchase = purchaseDao.findById(purchaseId) ?: return@withTransaction
        val txId = purchase.purchasedTransactionId ?: return@withTransaction
        val tx = transactionDao.findById(txId)
        // la marca de origen debe calzar para no revertir un movimiento equivocado
        if (tx == null || tx.originKey != "planned_purchase:$purchaseId") return@withTransaction
        val now = System.currentTimeMillis()
        purchaseDao.update(purchase.copy(status = PurchaseStatus.READY, purchasedTransactionId = null, updatedAt = now))
        transactionDao.delete(tx)
    }
}

private fun PlannedPurchaseEntity.toDomain() = PlannedPurchase(
    id = id, name = name, estimatedCostCents = estimatedCostCents, categoryId = categoryId,
    savingsGoalId = savingsGoalId, targetDate = targetDate?.let { LocalDate.ofEpochDay(it) },
    priority = priority, status = status, description = description, vendor = vendor,
    purchasedTransactionId = purchasedTransactionId, createdAt = createdAt, updatedAt = updatedAt,
)

private fun PlannedPurchase.toEntity(createdAt: Long, updatedAt: Long, purchasedTransactionId: Long? = this.purchasedTransactionId) = PlannedPurchaseEntity(
    id = id, name = name, estimatedCostCents = estimatedCostCents, categoryId = categoryId,
    savingsGoalId = savingsGoalId, targetDate = targetDate?.toEpochDay(), priority = priority, status = status,
    description = description, vendor = vendor, purchasedTransactionId = purchasedTransactionId,
    createdAt = createdAt, updatedAt = updatedAt,
)
