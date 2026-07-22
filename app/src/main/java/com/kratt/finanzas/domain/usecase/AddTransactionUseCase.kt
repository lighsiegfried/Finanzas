package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionDraft
import com.kratt.finanzas.domain.repository.TransactionRepository

sealed interface AddTransactionResult {
    data object Success : AddTransactionResult
    data class Invalid(val errors: Set<TransactionValidationError>) : AddTransactionResult
}

class AddTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val validateTransaction: ValidateTransactionUseCase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {

    // valida el borrador y guarda el movimiento en la base de datos
    suspend operator fun invoke(draft: TransactionDraft): AddTransactionResult {
        val errors = validateTransaction(draft)
        if (errors.isNotEmpty()) {
            return AddTransactionResult.Invalid(errors)
        }
        val now = nowMillis()
        transactionRepository.insert(
            Transaction(
                id = 0L,
                accountId = draft.accountId!!,
                categoryId = draft.categoryId!!,
                type = draft.type,
                amountCents = draft.amountCents!!,
                description = draft.description?.trim()?.takeIf { it.isNotEmpty() },
                date = draft.date!!,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        return AddTransactionResult.Success
    }
}
