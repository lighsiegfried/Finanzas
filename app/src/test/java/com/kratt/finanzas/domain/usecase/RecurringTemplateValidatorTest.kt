package com.kratt.finanzas.domain.usecase

import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringTemplateValidatorTest {

    private val start = LocalDate.of(2026, 7, 1)

    @Test
    fun rejectsInvalidAmountIntervalAndName() {
        val errors = RecurringTemplateValidator.validate("", null, null, 0, 0, null, null)
        assertTrue(RecurringValidationError.EMPTY_NAME in errors)
        assertTrue(RecurringValidationError.MISSING_ACCOUNT in errors)
        assertTrue(RecurringValidationError.MISSING_CATEGORY in errors)
        assertTrue(RecurringValidationError.INVALID_AMOUNT in errors)
        assertTrue(RecurringValidationError.INVALID_INTERVAL in errors)
        assertTrue(RecurringValidationError.INVALID_DATE in errors)
    }

    @Test
    fun rejectsEndBeforeStart() {
        val errors = RecurringTemplateValidator.validate("Internet", 1, 1, 20_000, 1, start, start.minusDays(1))
        assertTrue(RecurringValidationError.INVALID_END_DATE in errors)
    }

    @Test
    fun acceptsValidTemplate() {
        assertTrue(RecurringTemplateValidator.validate("Internet", 1, 1, 20_000, 1, start, null).isEmpty())
        assertTrue(RecurringTemplateValidator.validate("Salario", 1, 1, 500_000, 1, start, start.plusYears(1)).isEmpty())
    }
}
