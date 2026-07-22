package com.kratt.finanzas.data.repository

import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.entity.BudgetEntity
import com.kratt.finanzas.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private fun BudgetEntity.toDomain(): Budget = Budget(
    id = id, year = year, month = month, categoryId = categoryId, limitAmountCents = limitAmountCents,
    warningPercentage = warningPercentage, createdAtMillis = createdAt, updatedAtMillis = updatedAt,
)

// administra los presupuestos mensuales, general y por categoria
class BudgetRepository(
    database: AppDatabase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val budgetDao = database.budgetDao()

    fun observeForMonth(year: Int, month: Int): Flow<List<Budget>> =
        budgetDao.observeForMonth(year, month).map { list -> list.map { it.toDomain() } }

    suspend fun findById(id: Long): Budget? = budgetDao.findById(id)?.toDomain()

    suspend fun overallForMonth(year: Int, month: Int): Budget? =
        budgetDao.overallForMonth(year, month)?.toDomain()

    suspend fun categoryForMonth(year: Int, month: Int, categoryId: Long): Budget? =
        budgetDao.categoryForMonth(year, month, categoryId)?.toDomain()

    // ya existe un presupuesto para ese alcance y mes
    suspend fun exists(year: Int, month: Int, categoryId: Long?): Boolean =
        if (categoryId == null) overallForMonth(year, month) != null else categoryForMonth(year, month, categoryId) != null

    suspend fun createBudget(year: Int, month: Int, categoryId: Long?, limitCents: Long, warningPercentage: Int): Long {
        val now = nowMillis()
        return budgetDao.insert(
            BudgetEntity(
                year = year, month = month, categoryId = categoryId, limitAmountCents = limitCents,
                warningPercentage = warningPercentage, createdAt = now, updatedAt = now,
            ),
        )
    }

    suspend fun updateBudget(budget: Budget) {
        val existing = budgetDao.findById(budget.id) ?: return
        budgetDao.update(
            existing.copy(
                limitAmountCents = budget.limitAmountCents, warningPercentage = budget.warningPercentage, updatedAt = nowMillis(),
            ),
        )
    }

    suspend fun deleteBudget(id: Long) = budgetDao.deleteById(id)
}
