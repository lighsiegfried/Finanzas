package com.kratt.finanzas.data.repository

import com.kratt.finanzas.data.local.dao.AccountDao
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl(
    private val accountDao: AccountDao,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : AccountRepository {

    override fun observeActiveAccounts(): Flow<List<Account>> =
        accountDao.observeActive().map { entities -> entities.map { it.toDomain() } }

    override fun observeAllAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun findById(id: Long): Account? =
        accountDao.findById(id)?.toDomain()

    // registra la cuenta con las marcas de tiempo actuales
    override suspend fun insert(account: Account): Long {
        val now = nowMillis()
        return accountDao.insert(account.toEntity().copy(createdAt = now, updatedAt = now))
    }

    // actualiza la cuenta conservando createdAt y refrescando updatedAt
    override suspend fun update(account: Account) {
        accountDao.update(account.toEntity().copy(updatedAt = nowMillis()))
    }

    override suspend fun setActive(id: Long, active: Boolean) {
        accountDao.setActive(id, active, nowMillis())
    }

    override suspend fun delete(id: Long) {
        accountDao.deleteById(id)
    }
}
