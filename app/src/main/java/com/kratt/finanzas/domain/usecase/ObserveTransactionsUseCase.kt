package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.TransactionListItem
import com.kratt.finanzas.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class ObserveTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
) {

    operator fun invoke(): Flow<List<TransactionListItem>> =
        transactionRepository.observeAll()
}
