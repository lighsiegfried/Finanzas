package com.kratt.finanzas.domain.repository

import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionListItem
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun insert(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun delete(transaction: Transaction)
    suspend fun findById(id: Long): Transaction?
    fun observeAll(): Flow<List<TransactionListItem>>
    fun observeRecent(limit: Int): Flow<List<TransactionListItem>>
    fun observeMonthly(month: YearMonth): Flow<List<Transaction>>
    fun observeMonthlyWithNames(month: YearMonth): Flow<List<TransactionListItem>>
    fun observeAllTransactions(): Flow<List<Transaction>>
    suspend fun countByCategory(categoryId: Long): Long
    suspend fun countByAccount(accountId: Long): Long
}
