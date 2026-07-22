package com.kratt.finanzas.domain.repository

import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeActiveByType(type: TransactionType): Flow<List<Category>>
    fun observeAllByType(type: TransactionType): Flow<List<Category>>
    suspend fun findById(id: Long): Category?
    suspend fun insert(category: Category): Long
    suspend fun update(category: Category)
    suspend fun setActive(id: Long, active: Boolean)
}
