package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.TransactionListItem
import com.kratt.finanzas.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class ObserveRecentTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val limit: Int = DEFAULT_LIMIT,
) {

    operator fun invoke(): Flow<List<TransactionListItem>> =
        transactionRepository.observeRecent(limit)

    companion object {
        const val DEFAULT_LIMIT = 5
    }
}
