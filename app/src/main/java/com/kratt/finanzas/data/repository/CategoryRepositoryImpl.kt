package com.kratt.finanzas.data.repository

import com.kratt.finanzas.data.local.dao.CategoryDao
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : CategoryRepository {

    override fun observeActiveByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.observeActiveByType(type).map { entities -> entities.map { it.toDomain() } }

    override fun observeAllByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.observeAllByType(type).map { entities -> entities.map { it.toDomain() } }

    override suspend fun findById(id: Long): Category? =
        categoryDao.findById(id)?.toDomain()

    override suspend fun insert(category: Category): Long {
        val now = nowMillis()
        return categoryDao.insert(category.toEntity().copy(createdAt = now, updatedAt = now))
    }

    override suspend fun update(category: Category) {
        categoryDao.update(category.toEntity().copy(updatedAt = nowMillis()))
    }

    override suspend fun setActive(id: Long, active: Boolean) {
        categoryDao.setActive(id, active, nowMillis())
    }
}
