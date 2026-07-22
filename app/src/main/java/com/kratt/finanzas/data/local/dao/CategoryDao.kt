package com.kratt.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kratt.finanzas.data.local.entity.CategoryEntity
import com.kratt.finanzas.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>): List<Long>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE isActive = 1 AND transactionType = :type ORDER BY id")
    fun observeActiveByType(type: TransactionType): Flow<List<CategoryEntity>>

    // todas las categorias del tipo, activas primero, para administrarlas
    @Query("SELECT * FROM categories WHERE transactionType = :type ORDER BY isActive DESC, id")
    fun observeAllByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findById(id: Long): CategoryEntity?

    // activa o desactiva una categoria sin borrar su historial
    @Query("UPDATE categories SET isActive = :active, updatedAt = :now WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, now: Long)
}
