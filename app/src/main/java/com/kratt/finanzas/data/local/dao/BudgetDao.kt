package com.kratt.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kratt.finanzas.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun findById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE year = :year AND month = :month ORDER BY categoryId IS NULL DESC, id")
    fun observeForMonth(year: Int, month: Int): Flow<List<BudgetEntity>>

    // el presupuesto general del mes, sin categoria
    @Query("SELECT * FROM budgets WHERE year = :year AND month = :month AND categoryId IS NULL LIMIT 1")
    suspend fun overallForMonth(year: Int, month: Int): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE year = :year AND month = :month AND categoryId = :categoryId LIMIT 1")
    suspend fun categoryForMonth(year: Int, month: Int, categoryId: Long): BudgetEntity?
}
