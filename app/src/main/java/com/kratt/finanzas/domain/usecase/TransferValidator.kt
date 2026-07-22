package com.kratt.finanzas.domain.usecase

// errores posibles del formulario de transferencia
enum class TransferValidationError {
    MISSING_SOURCE,
    MISSING_DESTINATION,
    SAME_ACCOUNT,
    INVALID_AMOUNT,
}

object TransferValidator {

    // valida que haya origen y destino distintos y un monto mayor a cero
    fun validate(
        sourceAccountId: Long?,
        destinationAccountId: Long?,
        amountCents: Long?,
    ): Set<TransferValidationError> = buildSet {
        if (sourceAccountId == null) add(TransferValidationError.MISSING_SOURCE)
        if (destinationAccountId == null) add(TransferValidationError.MISSING_DESTINATION)
        if (sourceAccountId != null && destinationAccountId != null && sourceAccountId == destinationAccountId) {
            add(TransferValidationError.SAME_ACCOUNT)
        }
        if (amountCents == null || amountCents <= 0L) add(TransferValidationError.INVALID_AMOUNT)
    }
}
