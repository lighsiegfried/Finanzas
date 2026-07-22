package com.kratt.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kratt.finanzas.data.local.entity.InstallmentOccurrenceEntity
import com.kratt.finanzas.data.local.entity.InstallmentPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentDao {

    @Insert
    suspend fun insertPlan(plan: InstallmentPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: InstallmentPlanEntity)

    @Query("SELECT * FROM installment_plans WHERE id = :id")
    suspend fun findPlan(id: Long): InstallmentPlanEntity?

    @Query("SELECT * FROM installment_plans ORDER BY createdAt DESC")
    fun observePlans(): Flow<List<InstallmentPlanEntity>>

    @Insert
    suspend fun insertOccurrences(occurrences: List<InstallmentOccurrenceEntity>): List<Long>

    @Update
    suspend fun updateOccurrence(occurrence: InstallmentOccurrenceEntity)

    @Query("SELECT * FROM installment_occurrences WHERE id = :id")
    suspend fun findOccurrence(id: Long): InstallmentOccurrenceEntity?

    @Query("SELECT * FROM installment_occurrences WHERE installmentPlanId = :planId ORDER BY sequenceNumber")
    fun observeOccurrences(planId: Long): Flow<List<InstallmentOccurrenceEntity>>

    @Query("SELECT * FROM installment_occurrences WHERE installmentPlanId = :planId ORDER BY sequenceNumber")
    suspend fun occurrencesForPlan(planId: Long): List<InstallmentOccurrenceEntity>

    // cuotas pendientes o vencidas que caen en un rango, para compromisos y recordatorios
    @Query(
        "SELECT * FROM installment_occurrences " +
            "WHERE status IN ('PENDING','OVERDUE') AND dueDate BETWEEN :start AND :end ORDER BY dueDate",
    )
    suspend fun occurrencesDueBetween(start: Long, end: Long): List<InstallmentOccurrenceEntity>

    @Query("SELECT * FROM installment_occurrences WHERE status IN ('PENDING','OVERDUE') ORDER BY dueDate")
    fun observePendingOccurrences(): Flow<List<InstallmentOccurrenceEntity>>

    @Query("SELECT COUNT(*) FROM installment_occurrences WHERE installmentPlanId = :planId AND status = 'PAID'")
    suspend fun countPaid(planId: Long): Int

    // marca vencidas las cuotas pendientes con fecha ya pasada
    @Query("UPDATE installment_occurrences SET status = 'OVERDUE', updatedAt = :now WHERE status = 'PENDING' AND dueDate < :today")
    suspend fun markOverdue(today: Long, now: Long)
}
