package com.kratt.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kratt.finanzas.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId ORDER BY createdAt ASC")
    fun observeForTransaction(transactionId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId ORDER BY createdAt ASC")
    suspend fun listForTransaction(transactionId: Long): List<AttachmentEntity>

    @Query("SELECT COUNT(*) FROM attachments WHERE transactionId = :transactionId")
    fun observeCountForTransaction(transactionId: Long): Flow<Int>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun findById(id: Long): AttachmentEntity?

    @Query("SELECT * FROM attachments ORDER BY createdAt ASC")
    suspend fun listAll(): List<AttachmentEntity>

    // resumen de almacenamiento: cantidad total y suma de tamanos
    @Query("SELECT COUNT(*) FROM attachments")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM attachments")
    fun observeTotalBytes(): Flow<Long>

    @Insert
    suspend fun insert(entity: AttachmentEntity): Long

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
