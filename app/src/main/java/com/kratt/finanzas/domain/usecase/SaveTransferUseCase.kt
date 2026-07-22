package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.TransactionRepository
import java.time.LocalDate

sealed interface TransferResult {
    data object Success : TransferResult
    data class Invalid(val errors: Set<TransferValidationError>) : TransferResult
}

class SaveTransferUseCase(
    private val transactionRepository: TransactionRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {

    // crea o edita una transferencia como una sola fila para garantizar atomicidad
    suspend operator fun invoke(
        id: Long,
        sourceAccountId: Long?,
        destinationAccountId: Long?,
        amountCents: Long?,
        date: LocalDate?,
        description: String?,
        createdAtMillis: Long?,
    ): TransferResult {
        val errors = TransferValidator.validate(sourceAccountId, destinationAccountId, amountCents)
        if (errors.isNotEmpty() || date == null) {
            return TransferResult.Invalid(errors)
        }
        val now = nowMillis()
        val transfer = Transaction(
            id = id,
            accountId = sourceAccountId!!,
            destinationAccountId = destinationAccountId,
            categoryId = null,
            type = TransactionType.TRANSFER,
            amountCents = amountCents!!,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            date = date,
            createdAtMillis = createdAtMillis ?: now,
            updatedAtMillis = now,
        )
        if (id == 0L) {
            transactionRepository.insert(transfer)
        } else {
            transactionRepository.update(transfer)
        }
        return TransferResult.Success
    }
}
