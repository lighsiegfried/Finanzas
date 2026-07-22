package com.kratt.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kratt.finanzas.data.local.entity.RecurringOccurrenceEntity
import com.kratt.finanzas.data.local.entity.RecurringTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {

    @Insert
    suspend fun insertTemplate(template: RecurringTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: RecurringTemplateEntity)

    @Query("SELECT * FROM recurring_templates WHERE id = :id")
    suspend fun findTemplate(id: Long): RecurringTemplateEntity?

    @Query("SELECT * FROM recurring_templates ORDER BY createdAt DESC")
    fun observeTemplates(): Flow<List<RecurringTemplateEntity>>

    @Query("SELECT * FROM recurring_templates WHERE isActive = 1")
    suspend fun activeTemplates(): List<RecurringTemplateEntity>

    @Insert
    suspend fun insertOccurrence(occurrence: RecurringOccurrenceEntity): Long

    @Update
    suspend fun updateOccurrence(occurrence: RecurringOccurrenceEntity)

    @Query("SELECT * FROM recurring_occurrences WHERE id = :id")
    suspend fun findOccurrence(id: Long): RecurringOccurrenceEntity?

    @Query("SELECT * FROM recurring_occurrences WHERE recurringTemplateId = :templateId ORDER BY scheduledDate")
    fun observeOccurrences(templateId: Long): Flow<List<RecurringOccurrenceEntity>>

    // sirve para no duplicar una ocurrencia de la misma plantilla y fecha
    @Query("SELECT * FROM recurring_occurrences WHERE recurringTemplateId = :templateId AND scheduledDate = :date LIMIT 1")
    suspend fun findByTemplateAndDate(templateId: Long, date: Long): RecurringOccurrenceEntity?

    // la proxima ocurrencia pendiente o vencida de la plantilla, para confirmar u omitir
    @Query(
        "SELECT * FROM recurring_occurrences WHERE recurringTemplateId = :templateId " +
            "AND status IN ('PENDING','OVERDUE') ORDER BY scheduledDate LIMIT 1",
    )
    suspend fun nextPending(templateId: Long): RecurringOccurrenceEntity?

    @Query(
        "SELECT * FROM recurring_occurrences " +
            "WHERE status IN ('PENDING','OVERDUE') AND scheduledDate BETWEEN :start AND :end ORDER BY scheduledDate",
    )
    suspend fun occurrencesDueBetween(start: Long, end: Long): List<RecurringOccurrenceEntity>

    // ocurrencias pendientes que ya llegaron a su fecha, para registrar en automatico
    @Query("SELECT * FROM recurring_occurrences WHERE status = 'PENDING' AND scheduledDate <= :today ORDER BY scheduledDate")
    suspend fun duePending(today: Long): List<RecurringOccurrenceEntity>

    @Query("SELECT * FROM recurring_occurrences WHERE status IN ('PENDING','OVERDUE') ORDER BY scheduledDate")
    fun observePendingOccurrences(): Flow<List<RecurringOccurrenceEntity>>

    @Query("UPDATE recurring_occurrences SET status = 'OVERDUE', updatedAt = :now WHERE status = 'PENDING' AND scheduledDate < :today")
    suspend fun markOverdue(today: Long, now: Long)

    // al editar una plantilla se borran solo las ocurrencias pendientes para regenerarlas
    @Query("DELETE FROM recurring_occurrences WHERE recurringTemplateId = :templateId AND status = 'PENDING'")
    suspend fun deletePendingForTemplate(templateId: Long)
}
