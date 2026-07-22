package com.kratt.finanzas.domain.usecase

import java.time.LocalDate

enum class RecurringValidationError {
    EMPTY_NAME,
    MISSING_ACCOUNT,
    MISSING_CATEGORY,
    INVALID_AMOUNT,
    INVALID_INTERVAL,
    INVALID_DATE,
    INVALID_END_DATE,
}

object RecurringTemplateValidator {

    // valida nombre, cuenta, categoria, monto, intervalo y fechas
    fun validate(
        name: String,
        accountId: Long?,
        categoryId: Long?,
        amountCents: Long?,
        interval: Int?,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): Set<RecurringValidationError> = buildSet {
        if (name.trim().isEmpty()) add(RecurringValidationError.EMPTY_NAME)
        if (accountId == null) add(RecurringValidationError.MISSING_ACCOUNT)
        if (categoryId == null) add(RecurringValidationError.MISSING_CATEGORY)
        if (amountCents == null || amountCents <= 0L) add(RecurringValidationError.INVALID_AMOUNT)
        if (interval == null || interval < 1) add(RecurringValidationError.INVALID_INTERVAL)
        if (startDate == null) add(RecurringValidationError.INVALID_DATE)
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            add(RecurringValidationError.INVALID_END_DATE)
        }
    }
}
