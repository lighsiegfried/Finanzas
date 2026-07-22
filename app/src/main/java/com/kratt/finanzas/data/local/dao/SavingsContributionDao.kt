package com.kratt.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kratt.finanzas.data.local.entity.SavingsContributionEntity
import kotlinx.coroutines.flow.Flow

// total de aportes por meta, calculado con agregacion sql
data class GoalTotal(val savingsGoalId: Long, val totalCents: Long)

@Dao
interface SavingsContributionDao {

    @Query("SELECT * FROM savings_contributions WHERE savingsGoalId = :goalId ORDER BY contributionDate DESC, id DESC")
    fun observeByGoal(goalId: Long): Flow<List<SavingsContributionEntity>>

    // suma reactiva de los aportes de una meta; nunca carga todos los aportes en memoria para el total
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM savings_contributions WHERE savingsGoalId = :goalId")
    fun observeTotalByGoal(goalId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM savings_contributions WHERE savingsGoalId = :goalId")
    suspend fun totalByGoal(goalId: Long): Long

    // total por meta para toda la lista, en una sola consulta
    @Query("SELECT savingsGoalId, COALESCE(SUM(amountCents), 0) AS totalCents FROM savings_contributions GROUP BY savingsGoalId")
    fun observeTotalsByGoal(): Flow<List<GoalTotal>>

    // aportes dentro de un rango de dias epoch, para agrupar por mes en el dominio
    @Query("SELECT * FROM savings_contributions WHERE contributionDate BETWEEN :startEpochDay AND :endEpochDay ORDER BY contributionDate ASC")
    suspend fun listBetween(startEpochDay: Long, endEpochDay: Long): List<SavingsContributionEntity>

    @Query("SELECT * FROM savings_contributions WHERE savingsGoalId = :goalId ORDER BY contributionDate ASC, id ASC")
    suspend fun listByGoal(goalId: Long): List<SavingsContributionEntity>

    @Query("SELECT * FROM savings_contributions WHERE id = :id")
    suspend fun findById(id: Long): SavingsContributionEntity?

    @Insert
    suspend fun insert(entity: SavingsContributionEntity): Long

    @Update
    suspend fun update(entity: SavingsContributionEntity)

    @Query("DELETE FROM savings_contributions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
