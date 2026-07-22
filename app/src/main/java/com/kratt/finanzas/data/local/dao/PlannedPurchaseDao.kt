package com.kratt.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kratt.finanzas.data.local.entity.PlannedPurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedPurchaseDao {

    @Query("SELECT * FROM planned_purchases ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlannedPurchaseEntity>>

    @Query("SELECT * FROM planned_purchases WHERE id = :id")
    fun observeById(id: Long): Flow<PlannedPurchaseEntity?>

    @Query("SELECT * FROM planned_purchases WHERE id = :id")
    suspend fun findById(id: Long): PlannedPurchaseEntity?

    // compra activa ligada a una meta, para la regla de una meta con una sola compra activa
    @Query("SELECT * FROM planned_purchases WHERE savingsGoalId = :goalId AND status NOT IN ('PURCHASED', 'CANCELLED', 'ARCHIVED')")
    suspend fun activeByGoal(goalId: Long): List<PlannedPurchaseEntity>

    @Insert
    suspend fun insert(entity: PlannedPurchaseEntity): Long

    @Update
    suspend fun update(entity: PlannedPurchaseEntity)

    @Query("UPDATE planned_purchases SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, now: Long)
}
