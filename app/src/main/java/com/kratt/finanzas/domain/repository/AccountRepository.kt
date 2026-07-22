package com.kratt.finanzas.domain.repository

import com.kratt.finanzas.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeActiveAccounts(): Flow<List<Account>>
    fun observeAllAccounts(): Flow<List<Account>>
    suspend fun findById(id: Long): Account?
    suspend fun insert(account: Account): Long
    suspend fun update(account: Account)
    suspend fun setActive(id: Long, active: Boolean)
    suspend fun delete(id: Long)
}
