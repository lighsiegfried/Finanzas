package com.kratt.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kratt.finanzas.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun observeById(id: Long): Flow<SavingsGoalEntity?>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun findById(id: Long): SavingsGoalEntity?

    @Insert
    suspend fun insert(entity: SavingsGoalEntity): Long

    @Update
    suspend fun update(entity: SavingsGoalEntity)

    @Query("UPDATE savings_goals SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, now: Long)

    @Query("UPDATE savings_goals SET isArchived = :archived, status = :status, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, status: String, now: Long)
}
