package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.TransactionDraft

// errores que puede tener el formulario de movimiento
enum class TransactionValidationError {
    INVALID_AMOUNT,
    AMOUNT_TOO_LARGE,
    MISSING_ACCOUNT,
    MISSING_CATEGORY,
    INVALID_DATE,
}

class ValidateTransactionUseCase {

    // revisa que el borrador tenga monto positivo, cuenta, categoria y fecha
    operator fun invoke(draft: TransactionDraft): Set<TransactionValidationError> = buildSet {
        val amount = draft.amountCents
        if (amount == null || amount <= 0L) {
            add(TransactionValidationError.INVALID_AMOUNT)
        } else if (!MoneyMath.isSupportedAmount(amount)) {
            // monto valido pero fuera del rango que se puede representar con seguridad
            add(TransactionValidationError.AMOUNT_TOO_LARGE)
        }
        if (draft.accountId == null) {
            add(TransactionValidationError.MISSING_ACCOUNT)
        }
        if (draft.categoryId == null) {
            add(TransactionValidationError.MISSING_CATEGORY)
        }
        if (draft.date == null) {
            add(TransactionValidationError.INVALID_DATE)
        }
    }
}
