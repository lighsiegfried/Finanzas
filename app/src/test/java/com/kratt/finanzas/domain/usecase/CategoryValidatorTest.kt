package com.kratt.finanzas.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryValidatorTest {

    @Test
    fun emptyNameRejected() {
        assertTrue(CategoryValidationError.EMPTY_NAME in CategoryValidator.validate("  ", emptySet()))
    }

    @Test
    fun duplicateNameIsCaseInsensitive() {
        assertTrue(CategoryValidationError.DUPLICATE_NAME in CategoryValidator.validate("Salario", setOf("salario")))
    }

    @Test
    fun validNameHasNoErrors() {
        assertTrue(CategoryValidator.validate("Regalos", setOf("salario")).isEmpty())
    }

    @Test
    fun typeChangeBlockedWhenMovementsExist() {
        assertFalse(CategoryValidator.canChangeType(hasMovements = true))
        assertTrue(CategoryValidator.canChangeType(hasMovements = false))
    }
}
