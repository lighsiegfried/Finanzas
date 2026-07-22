package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.TransactionTemplate
import com.kratt.finanzas.domain.model.TransactionType

// errores de validacion de una plantilla
enum class TemplateValidationError {
    NAME_REQUIRED,
    ACCOUNT_REQUIRED,
    CATEGORY_REQUIRED,
    SAME_ACCOUNTS,
    INVALID_AMOUNT,
    DUPLICATE_NAME,
}

// valida la forma de una plantilla; el nombre duplicado se confirma contra la base en el repositorio
object TemplateValidator {

    // revisa las reglas estructurales, sin tocar la base
    fun validate(template: TransactionTemplate): Set<TemplateValidationError> {
        val errors = mutableSetOf<TemplateValidationError>()
        if (template.name.isBlank()) errors += TemplateValidationError.NAME_REQUIRED
        if (template.accountId <= 0L) errors += TemplateValidationError.ACCOUNT_REQUIRED

        when (template.type) {
            TransactionType.TRANSFER -> {
                // una transferencia necesita cuenta destino distinta y no usa categoria
                if (template.destinationAccountId == null || template.destinationAccountId <= 0L) {
                    errors += TemplateValidationError.ACCOUNT_REQUIRED
                } else if (template.destinationAccountId == template.accountId) {
                    errors += TemplateValidationError.SAME_ACCOUNTS
                }
            }
            TransactionType.EXPENSE, TransactionType.INCOME -> {
                if (template.categoryId == null || template.categoryId <= 0L) {
                    errors += TemplateValidationError.CATEGORY_REQUIRED
                }
            }
        }

        // el monto es opcional, pero si viene debe ser positivo y sin desborde
        val amount = template.defaultAmountCents
        if (amount != null && (amount <= 0L || amount > MoneyMath.MAX_SUPPORTED_CENTS)) {
            errors += TemplateValidationError.INVALID_AMOUNT
        }
        return errors
    }
}
