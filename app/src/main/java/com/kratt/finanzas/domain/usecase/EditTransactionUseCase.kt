package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionDraft
import com.kratt.finanzas.domain.repository.TransactionRepository

class EditTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val validateTransaction: ValidateTransactionUseCase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {

    // valida y actualiza un movimiento conservando createdAt y refrescando updatedAt
    suspend operator fun invoke(existing: Transaction, draft: TransactionDraft): AddTransactionResult {
        val errors = validateTransaction(draft)
        if (errors.isNotEmpty()) {
            return AddTransactionResult.Invalid(errors)
        }
        transactionRepository.update(
            existing.copy(
                accountId = draft.accountId!!,
                categoryId = draft.categoryId,
                destinationAccountId = null,
                type = draft.type,
                amountCents = draft.amountCents!!,
                description = draft.description?.trim()?.takeIf { it.isNotEmpty() },
                date = draft.date!!,
                updatedAtMillis = nowMillis(),
            ),
        )
        return AddTransactionResult.Success
    }
}
