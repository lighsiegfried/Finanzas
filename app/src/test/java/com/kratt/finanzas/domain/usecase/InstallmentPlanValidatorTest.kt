package com.kratt.finanzas.domain.usecase

import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallmentPlanValidatorTest {

    private val date = LocalDate.of(2026, 7, 20)

    @Test
    fun rejectsEmptyNameAndMissingRefs() {
        val errors = InstallmentPlanValidator.validate("  ", null, null, 1000, 12, date)
        assertTrue(InstallmentValidationError.EMPTY_NAME in errors)
        assertTrue(InstallmentValidationError.MISSING_ACCOUNT in errors)
        assertTrue(InstallmentValidationError.MISSING_CATEGORY in errors)
    }

    @Test
    fun rejectsInvalidTotalAndCount() {
        assertTrue(InstallmentValidationError.INVALID_TOTAL in InstallmentPlanValidator.validate("A", 1, 1, 0, 12, date))
        assertTrue(InstallmentValidationError.INVALID_COUNT in InstallmentPlanValidator.validate("A", 1, 1, 1000, 1, date))
        assertTrue(InstallmentValidationError.INVALID_COUNT in InstallmentPlanValidator.validate("A", 1, 1, 1000, 121, date))
    }

    @Test
    fun acceptsValidPlan() {
        assertTrue(InstallmentPlanValidator.validate("Monitor", 1, 1, 153_600, 12, date).isEmpty())
    }
}
