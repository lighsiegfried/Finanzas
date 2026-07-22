package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepository,
) {
    // borra el movimiento; al ser una sola fila la transferencia tambien es atomica
    // devuelve false si el movimiento fue generado y solo puede quitarse con su reverso
    suspend operator fun invoke(transaction: Transaction): Boolean {
        if (transaction.isGenerated) return false
        transactionRepository.delete(transaction)
        return true
    }
}
