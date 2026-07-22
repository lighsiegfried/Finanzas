package com.kratt.finanzas.domain.usecase

import java.time.LocalDate

enum class InstallmentValidationError {
    EMPTY_NAME,
    MISSING_ACCOUNT,
    MISSING_CATEGORY,
    INVALID_TOTAL,
    INVALID_COUNT,
    INVALID_DATE,
}

object InstallmentPlanValidator {

    const val MIN_COUNT = 2
    const val MAX_COUNT = 120

    // valida el nombre, la cuenta, la categoria, el total y la cantidad de cuotas
    fun validate(
        name: String,
        accountId: Long?,
        categoryId: Long?,
        totalCents: Long?,
        installmentCount: Int?,
        firstDueDate: LocalDate?,
    ): Set<InstallmentValidationError> = buildSet {
        if (name.trim().isEmpty()) add(InstallmentValidationError.EMPTY_NAME)
        if (accountId == null) add(InstallmentValidationError.MISSING_ACCOUNT)
        if (categoryId == null) add(InstallmentValidationError.MISSING_CATEGORY)
        if (totalCents == null || totalCents <= 0L) add(InstallmentValidationError.INVALID_TOTAL)
        if (installmentCount == null || installmentCount < MIN_COUNT || installmentCount > MAX_COUNT) {
            add(InstallmentValidationError.INVALID_COUNT)
        }
        if (firstDueDate == null) add(InstallmentValidationError.INVALID_DATE)
    }
}
