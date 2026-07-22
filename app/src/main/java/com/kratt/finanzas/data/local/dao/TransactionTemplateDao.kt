package com.kratt.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kratt.finanzas.data.local.entity.TransactionTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTemplateDao {

    @Query("SELECT * FROM transaction_templates WHERE isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<TransactionTemplateEntity>>

    @Query("SELECT * FROM transaction_templates ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TransactionTemplateEntity>>

    @Query("SELECT * FROM transaction_templates WHERE isActive = 1 AND isFavorite = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<TransactionTemplateEntity>>

    // plantillas usadas recientemente, las mas recientes primero
    @Query("SELECT * FROM transaction_templates WHERE isActive = 1 AND lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionTemplateEntity>>

    @Query("SELECT * FROM transaction_templates WHERE id = :id")
    suspend fun findById(id: Long): TransactionTemplateEntity?

    // cuenta plantillas activas con el mismo nombre, ignorando la que se edita
    @Query("SELECT COUNT(*) FROM transaction_templates WHERE isActive = 1 AND name = :name COLLATE NOCASE AND id != :excludeId")
    suspend fun countActiveByName(name: String, excludeId: Long): Int

    @Insert
    suspend fun insert(entity: TransactionTemplateEntity): Long

    @Update
    suspend fun update(entity: TransactionTemplateEntity)

    @Query("UPDATE transaction_templates SET lastUsedAt = :usedAt, updatedAt = :usedAt WHERE id = :id")
    suspend fun markUsed(id: Long, usedAt: Long)

    @Query("UPDATE transaction_templates SET isActive = :active, updatedAt = :now WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, now: Long)

    @Query("UPDATE transaction_templates SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, now: Long)
}
