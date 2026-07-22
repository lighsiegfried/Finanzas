package com.kratt.finanzas.domain.usecase

// errores posibles del formulario de cuenta
enum class AccountValidationError {
    EMPTY_NAME,
    DUPLICATE_NAME,
    INVALID_AMOUNT,
    INVALID_LAST_FOUR,
}

object AccountValidator {

    // valida el nombre, el saldo inicial y los ultimos cuatro digitos
    // existingActiveNamesLower trae los nombres activos en minusculas sin la cuenta que se edita
    fun validate(
        name: String,
        initialBalanceCents: Long?,
        lastFourDigits: String?,
        existingActiveNamesLower: Set<String>,
    ): Set<AccountValidationError> = buildSet {
        val trimmed = name.trim()
        when {
            trimmed.isEmpty() -> add(AccountValidationError.EMPTY_NAME)
            existingActiveNamesLower.contains(trimmed.lowercase()) -> add(AccountValidationError.DUPLICATE_NAME)
        }
        if (initialBalanceCents == null) {
            add(AccountValidationError.INVALID_AMOUNT)
        }
        val digits = lastFourDigits?.trim()
        if (!digits.isNullOrEmpty() && !(digits.length == 4 && digits.all { it.isDigit() })) {
            add(AccountValidationError.INVALID_LAST_FOUR)
        }
    }
}
