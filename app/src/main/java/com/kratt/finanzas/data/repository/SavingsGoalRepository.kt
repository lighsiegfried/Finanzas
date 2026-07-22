package com.kratt.finanzas.data.repository

import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.dao.GoalTotal
import com.kratt.finanzas.data.local.entity.SavingsGoalEntity
import com.kratt.finanzas.domain.model.SavingsGoal
import com.kratt.finanzas.domain.model.SavingsGoalStatus
import com.kratt.finanzas.domain.usecase.SavingsGoalValidationError
import com.kratt.finanzas.domain.usecase.SavingsGoalValidator
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface GoalSaveResult {
    data class Success(val id: Long) : GoalSaveResult
    data class Invalid(val errors: Set<SavingsGoalValidationError>) : GoalSaveResult
}

class SavingsGoalRepository(private val database: AppDatabase) {

    private val goalDao = database.savingsGoalDao()
    private val contributionDao = database.savingsContributionDao()

    fun observeAll(): Flow<List<SavingsGoal>> = goalDao.observeAll().map { list -> list.map { it.toDomain() } }
    fun observeById(id: Long): Flow<SavingsGoal?> = goalDao.observeById(id).map { it?.toDomain() }
    fun observeTotalsByGoal(): Flow<List<GoalTotal>> = contributionDao.observeTotalsByGoal()
    fun observeTotalByGoal(id: Long): Flow<Long> = contributionDao.observeTotalByGoal(id)

    suspend fun findById(id: Long): SavingsGoal? = goalDao.findById(id)?.toDomain()
    suspend fun totalByGoal(id: Long): Long = contributionDao.totalByGoal(id)

    // valida y guarda la meta
    suspend fun save(goal: SavingsGoal): GoalSaveResult {
        val trimmed = goal.copy(name = goal.name.trim(), description = goal.description?.trim()?.ifBlank { null })
        val errors = SavingsGoalValidator.validate(trimmed)
        if (errors.isNotEmpty()) return GoalSaveResult.Invalid(errors)
        val now = System.currentTimeMillis()
        return if (trimmed.id == 0L) {
            GoalSaveResult.Success(goalDao.insert(trimmed.toEntity(createdAt = now, updatedAt = now)))
        } else {
            val existing = goalDao.findById(trimmed.id) ?: return GoalSaveResult.Invalid(emptySet())
            goalDao.update(trimmed.toEntity(createdAt = existing.createdAt, updatedAt = now))
            GoalSaveResult.Success(trimmed.id)
        }
    }

    suspend fun setStatus(id: Long, status: SavingsGoalStatus) =
        goalDao.setStatus(id, status.name, System.currentTimeMillis())

    // archivar tambien deja el estado archivado
    suspend fun setArchived(id: Long, archived: Boolean) {
        val status = if (archived) SavingsGoalStatus.ARCHIVED else SavingsGoalStatus.ACTIVE
        goalDao.setArchived(id, archived, status.name, System.currentTimeMillis())
    }
}

private fun SavingsGoalEntity.toDomain() = SavingsGoal(
    id = id, name = name, targetAmountCents = targetAmountCents, linkedAccountId = linkedAccountId,
    startDate = LocalDate.ofEpochDay(startDate), targetDate = targetDate?.let { LocalDate.ofEpochDay(it) },
    status = status, description = description, iconKey = iconKey, colorKey = colorKey,
    isArchived = isArchived, createdAt = createdAt, updatedAt = updatedAt,
)

private fun SavingsGoal.toEntity(createdAt: Long, updatedAt: Long) = SavingsGoalEntity(
    id = id, name = name, targetAmountCents = targetAmountCents, linkedAccountId = linkedAccountId,
    startDate = startDate.toEpochDay(), targetDate = targetDate?.toEpochDay(),
    status = status, description = description, iconKey = iconKey, colorKey = colorKey,
    isArchived = isArchived, createdAt = createdAt, updatedAt = updatedAt,
)
