package com.kratt.finanzas.data.repository

import com.kratt.finanzas.data.local.dao.TransactionDao
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionListItem
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.MonthRange
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
) : TransactionRepository {

    override suspend fun insert(transaction: Transaction): Long =
        transactionDao.insert(transaction.toEntity())

    override suspend fun update(transaction: Transaction) =
        transactionDao.update(transaction.toEntity())

    override suspend fun delete(transaction: Transaction) =
        transactionDao.delete(transaction.toEntity())

    override suspend fun findById(id: Long): Transaction? =
        transactionDao.findById(id)?.toDomain()

    override fun observeAll(): Flow<List<TransactionListItem>> =
        transactionDao.observeAllWithNames().map { rows -> rows.map { it.toDomain() } }

    override fun observeRecent(limit: Int): Flow<List<TransactionListItem>> =
        transactionDao.observeRecentWithNames(limit).map { rows -> rows.map { it.toDomain() } }

    // consulta el mes usando el rango de dias en epoch day
    override fun observeMonthly(month: YearMonth): Flow<List<Transaction>> {
        val range = MonthRange.of(month)
        return transactionDao.observeBetween(range.startEpochDay, range.endEpochDay)
            .map { entities -> entities.map { it.toDomain() } }
    }

    // movimientos del mes con nombres, para la lista y los filtros
    override fun observeMonthlyWithNames(month: YearMonth): Flow<List<TransactionListItem>> {
        val range = MonthRange.of(month)
        return transactionDao.observeBetweenWithNames(range.startEpochDay, range.endEpochDay)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeAllTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun countByCategory(categoryId: Long): Long =
        transactionDao.countByCategory(categoryId)

    override suspend fun countByAccount(accountId: Long): Long =
        transactionDao.countByAccount(accountId)
}
