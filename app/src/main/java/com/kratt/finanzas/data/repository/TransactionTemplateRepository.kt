package com.kratt.finanzas.data.repository

import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.entity.TransactionTemplateEntity
import com.kratt.finanzas.domain.model.TransactionTemplate
import com.kratt.finanzas.domain.usecase.TemplateValidationError
import com.kratt.finanzas.domain.usecase.TemplateValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// resultado de guardar una plantilla, con los errores para la ui
sealed interface TemplateSaveResult {
    data class Success(val id: Long) : TemplateSaveResult
    data class Invalid(val errors: Set<TemplateValidationError>) : TemplateSaveResult
}

class TransactionTemplateRepository(private val db: AppDatabase) {

    private val dao = db.transactionTemplateDao()

    fun observeActive(): Flow<List<TransactionTemplate>> = dao.observeActive().map { list -> list.map { it.toDomain() } }
    fun observeAll(): Flow<List<TransactionTemplate>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    fun observeFavorites(): Flow<List<TransactionTemplate>> = dao.observeFavorites().map { list -> list.map { it.toDomain() } }
    fun observeRecent(limit: Int = 5): Flow<List<TransactionTemplate>> = dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    suspend fun findById(id: Long): TransactionTemplate? = dao.findById(id)?.toDomain()

    // valida forma y nombre unico antes de guardar
    suspend fun save(template: TransactionTemplate): TemplateSaveResult {
        val trimmed = template.copy(name = template.name.trim(), description = template.description?.trim()?.ifBlank { null })
        val errors = TemplateValidator.validate(trimmed).toMutableSet()
        if (errors.isEmpty() && dao.countActiveByName(trimmed.name, trimmed.id) > 0) {
            errors += TemplateValidationError.DUPLICATE_NAME
        }
        if (errors.isNotEmpty()) return TemplateSaveResult.Invalid(errors)

        val now = System.currentTimeMillis()
        return if (trimmed.id == 0L) {
            val id = dao.insert(trimmed.toEntity(createdAt = now, updatedAt = now))
            TemplateSaveResult.Success(id)
        } else {
            val existing = dao.findById(trimmed.id) ?: return TemplateSaveResult.Invalid(emptySet())
            dao.update(trimmed.toEntity(createdAt = existing.createdAt, updatedAt = now))
            TemplateSaveResult.Success(trimmed.id)
        }
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite, System.currentTimeMillis())
    suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active, System.currentTimeMillis())

    // marca la plantilla como usada para el orden de recientes; no toca el historial de movimientos
    suspend fun markUsed(id: Long) = dao.markUsed(id, System.currentTimeMillis())
}

private fun TransactionTemplateEntity.toDomain() = TransactionTemplate(
    id = id, name = name, type = transactionType, accountId = accountId,
    destinationAccountId = destinationAccountId, categoryId = categoryId,
    defaultAmountCents = defaultAmountCents, description = description,
    isFavorite = isFavorite, isActive = isActive, lastUsedAt = lastUsedAt,
    createdAt = createdAt, updatedAt = updatedAt,
)

private fun TransactionTemplate.toEntity(createdAt: Long, updatedAt: Long) = TransactionTemplateEntity(
    id = id, name = name, transactionType = type, accountId = accountId,
    destinationAccountId = if (type == com.kratt.finanzas.domain.model.TransactionType.TRANSFER) destinationAccountId else null,
    categoryId = if (type == com.kratt.finanzas.domain.model.TransactionType.TRANSFER) null else categoryId,
    defaultAmountCents = defaultAmountCents, description = description,
    isFavorite = isFavorite, isActive = isActive, lastUsedAt = lastUsedAt,
    createdAt = createdAt, updatedAt = updatedAt,
)
