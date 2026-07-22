package com.kratt.finanzas.domain.usecase

import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetValidatorTest {

    @Test
    fun invalidAmountRejected() {
        assertTrue(BudgetValidationError.INVALID_AMOUNT in BudgetValidator.validate(0, false, null, 80, false))
        assertTrue(BudgetValidationError.INVALID_AMOUNT in BudgetValidator.validate(null, false, null, 80, false))
    }

    @Test
    fun categoryBudgetRequiresCategory() {
        assertTrue(BudgetValidationError.MISSING_CATEGORY in BudgetValidator.validate(1000, true, null, 80, false))
    }

    @Test
    fun duplicateRejected() {
        assertTrue(BudgetValidationError.DUPLICATE in BudgetValidator.validate(1000, true, 1, 80, true))
    }

    @Test
    fun warningOutOfRangeRejected() {
        assertTrue(BudgetValidationError.INVALID_WARNING in BudgetValidator.validate(1000, false, null, 0, false))
        assertTrue(BudgetValidationError.INVALID_WARNING in BudgetValidator.validate(1000, false, null, 101, false))
    }

    @Test
    fun validOverallBudgetHasNoErrors() {
        assertTrue(BudgetValidator.validate(600_000, false, null, 80, false).isEmpty())
    }
}
