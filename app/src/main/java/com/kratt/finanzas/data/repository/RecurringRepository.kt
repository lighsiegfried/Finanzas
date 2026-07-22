package com.kratt.finanzas.data.repository

import androidx.room.withTransaction
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.local.entity.RecurringOccurrenceEntity
import com.kratt.finanzas.data.local.entity.RecurringTemplateEntity
import com.kratt.finanzas.data.local.entity.TransactionEntity
import com.kratt.finanzas.domain.model.PostingMode
import com.kratt.finanzas.domain.model.RecurrenceType
import com.kratt.finanzas.domain.model.RecurringOccurrence
import com.kratt.finanzas.domain.model.RecurringOccurrenceStatus
import com.kratt.finanzas.domain.model.RecurringTemplate
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.RecurrenceScheduler
import com.kratt.finanzas.domain.usecase.RecurringPostingRules
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface PostResult {
    data object Success : PostResult
    data object AlreadyPosted : PostResult
    data object NotFound : PostResult
}

// resultado de revertir una ocurrencia recurrente ya registrada
sealed interface RevertResult {
    data object Success : RevertResult
    data object NotPosted : RevertResult
    data object NotFound : RevertResult
    data object Mismatch : RevertResult
}

private const val HORIZON_DAYS = 92L

// administra plantillas recurrentes y genera sus ocurrencias de forma acotada e idempotente
class RecurringRepository(
    private val database: AppDatabase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val recurringDao = database.recurringDao()
    private val transactionDao = database.transactionDao()

    fun observeTemplates(): Flow<List<RecurringTemplate>> =
        recurringDao.observeTemplates().map { list -> list.map { it.toDomain() } }

    suspend fun findTemplate(id: Long): RecurringTemplate? = recurringDao.findTemplate(id)?.toDomain()

    fun observeOccurrences(templateId: Long): Flow<List<RecurringOccurrence>> =
        recurringDao.observeOccurrences(templateId).map { list -> list.map { it.toDomain() } }

    // crea la plantilla y genera las primeras ocurrencias dentro de la ventana
    suspend fun createTemplate(
        name: String,
        transactionType: TransactionType,
        accountId: Long,
        categoryId: Long,
        amountCents: Long,
        recurrenceType: RecurrenceType,
        interval: Int,
        startDate: LocalDate,
        endDate: LocalDate?,
        postingMode: PostingMode,
        description: String?,
        today: LocalDate,
    ): Long = database.withTransaction {
        val now = nowMillis()
        val templateId = recurringDao.insertTemplate(
            RecurringTemplateEntity(
                name = name.trim(), transactionType = transactionType, accountId = accountId, categoryId = categoryId,
                amountCents = amountCents, recurrenceType = recurrenceType, interval = interval,
                startDate = startDate.toEpochDay(), endDate = endDate?.toEpochDay(),
                nextOccurrenceDate = startDate.toEpochDay(), postingMode = postingMode, isActive = true,
                description = description?.trim()?.takeIf { it.isNotEmpty() }, createdAt = now, updatedAt = now,
            ),
        )
        generateForTemplate(recurringDao.findTemplate(templateId)!!, today, now)
        templateId
    }

    suspend fun updateTemplate(template: RecurringTemplate) = database.withTransaction {
        val entity = recurringDao.findTemplate(template.id) ?: return@withTransaction
        recurringDao.updateTemplate(
            entity.copy(
                name = template.name.trim(), transactionType = template.transactionType, accountId = template.accountId,
                categoryId = template.categoryId, amountCents = template.amountCents, recurrenceType = template.recurrenceType,
                interval = template.interval, startDate = template.startDate.toEpochDay(),
                endDate = template.endDate?.toEpochDay(), postingMode = template.postingMode, isActive = template.isActive,
                description = template.description?.trim()?.takeIf { it.isNotEmpty() }, updatedAt = nowMillis(),
            ),
        )
        // regenera solo las pendientes, nunca reescribe las ya registradas
        recurringDao.deletePendingForTemplate(template.id)
    }

    suspend fun setActive(templateId: Long, active: Boolean) {
        val template = recurringDao.findTemplate(templateId) ?: return
        recurringDao.updateTemplate(template.copy(isActive = active, updatedAt = nowMillis()))
    }

    // genera las ocurrencias faltantes de todas las plantillas activas, idempotente
    suspend fun generateDueOccurrences(today: LocalDate) = database.withTransaction {
        val now = nowMillis()
        recurringDao.activeTemplates().forEach { template -> generateForTemplate(template, today, now) }
        recurringDao.markOverdue(today.toEpochDay(), now)
    }

    private suspend fun generateForTemplate(template: RecurringTemplateEntity, today: LocalDate, now: Long) {
        val horizon = today.plusDays(HORIZON_DAYS)
        val dates = RecurrenceScheduler.scheduledDates(
            startDate = LocalDate.ofEpochDay(template.startDate),
            type = template.recurrenceType,
            interval = template.interval,
            endDate = template.endDate?.let(LocalDate::ofEpochDay),
            horizon = horizon,
        )
        for (date in dates) {
            if (recurringDao.findByTemplateAndDate(template.id, date.toEpochDay()) == null) {
                recurringDao.insertOccurrence(
                    RecurringOccurrenceEntity(
                        recurringTemplateId = template.id, scheduledDate = date.toEpochDay(), amountCents = template.amountCents,
                        status = RecurringOccurrenceStatus.PENDING, createdAt = now, updatedAt = now,
                    ),
                )
            }
        }
        val next = dates.firstOrNull { !it.isBefore(today) } ?: LocalDate.ofEpochDay(template.nextOccurrenceDate)
        recurringDao.updateTemplate(template.copy(nextOccurrenceDate = next.toEpochDay(), updatedAt = now))
    }

    // registra una ocurrencia confirmada como movimiento real, una sola vez
    suspend fun postOccurrence(occurrenceId: Long): PostResult = database.withTransaction {
        val occurrence = recurringDao.findOccurrence(occurrenceId) ?: return@withTransaction PostResult.NotFound
        if (!RecurringPostingRules.canPost(occurrence.status)) return@withTransaction PostResult.AlreadyPosted
        val template = recurringDao.findTemplate(occurrence.recurringTemplateId) ?: return@withTransaction PostResult.NotFound
        postInternal(occurrence, template)
        PostResult.Success
    }

    // registra en automatico las ocurrencias vencidas de plantillas con auto-post, exactamente una vez
    suspend fun autoPostDue(today: LocalDate) = database.withTransaction {
        val autoTemplates = recurringDao.activeTemplates()
            .filter { it.postingMode == PostingMode.AUTO_POST }
            .associateBy { it.id }
        recurringDao.duePending(today.toEpochDay()).forEach { occurrence ->
            val template = autoTemplates[occurrence.recurringTemplateId]
            if (template != null && RecurringPostingRules.canPost(occurrence.status)) {
                postInternal(occurrence, template)
            }
        }
    }

    private suspend fun postInternal(occurrence: RecurringOccurrenceEntity, template: RecurringTemplateEntity) {
        val now = nowMillis()
        val transactionId = transactionDao.insert(
            TransactionEntity(
                accountId = template.accountId, categoryId = template.categoryId, type = template.transactionType,
                amountCents = occurrence.amountCents, description = template.name, transactionDate = occurrence.scheduledDate,
                createdAt = now, updatedAt = now, originKey = "recurring:${occurrence.id}",
            ),
        )
        recurringDao.updateOccurrence(
            occurrence.copy(status = RecurringOccurrenceStatus.POSTED, generatedTransactionId = transactionId, updatedAt = now),
        )
    }

    // revierte una ocurrencia registrada: desvincula, borra el movimiento generado y vuelve a pendiente
    // todo en una sola transaccion para no dejar ocurrencias huerfanas si el proceso muere
    suspend fun revertOccurrence(occurrenceId: Long): RevertResult = database.withTransaction {
        val occurrence = recurringDao.findOccurrence(occurrenceId) ?: return@withTransaction RevertResult.NotFound
        if (occurrence.status != RecurringOccurrenceStatus.POSTED) return@withTransaction RevertResult.NotPosted
        val transactionId = occurrence.generatedTransactionId ?: return@withTransaction RevertResult.NotFound
        val transaction = transactionDao.findById(transactionId) ?: return@withTransaction RevertResult.NotFound
        // solo se revierte si el movimiento realmente pertenece a esta ocurrencia
        if (transaction.originKey != "recurring:${occurrence.id}") return@withTransaction RevertResult.Mismatch
        val now = nowMillis()
        // primero se desvincula la ocurrencia para poder borrar el movimiento con la fk restrict
        recurringDao.updateOccurrence(
            occurrence.copy(status = RecurringOccurrenceStatus.PENDING, generatedTransactionId = null, updatedAt = now),
        )
        transactionDao.delete(transaction)
        RevertResult.Success
    }

    suspend fun skipOccurrence(occurrenceId: Long) {
        val occurrence = recurringDao.findOccurrence(occurrenceId) ?: return
        if (occurrence.status == RecurringOccurrenceStatus.PENDING || occurrence.status == RecurringOccurrenceStatus.OVERDUE) {
            recurringDao.updateOccurrence(occurrence.copy(status = RecurringOccurrenceStatus.SKIPPED, updatedAt = nowMillis()))
        }
    }

    suspend fun occurrencesDueBetween(start: LocalDate, end: LocalDate): List<RecurringOccurrence> =
        recurringDao.occurrencesDueBetween(start.toEpochDay(), end.toEpochDay()).map { it.toDomain() }

    suspend fun nextPending(templateId: Long): RecurringOccurrence? =
        recurringDao.nextPending(templateId)?.toDomain()
}
