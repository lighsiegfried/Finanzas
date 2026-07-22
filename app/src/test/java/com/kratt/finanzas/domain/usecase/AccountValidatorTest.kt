package com.kratt.finanzas.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountValidatorTest {

    @Test
    fun emptyNameRejected() {
        val errors = AccountValidator.validate("   ", 0L, null, emptySet())
        assertTrue(AccountValidationError.EMPTY_NAME in errors)
    }

    @Test
    fun duplicateNameIsCaseInsensitive() {
        val errors = AccountValidator.validate("Efectivo", 0L, null, setOf("efectivo"))
        assertTrue(AccountValidationError.DUPLICATE_NAME in errors)
    }

    @Test
    fun invalidAmountRejected() {
        val errors = AccountValidator.validate("Banco", null, null, emptySet())
        assertTrue(AccountValidationError.INVALID_AMOUNT in errors)
    }

    @Test
    fun lastFourMustBeExactlyFourDigits() {
        assertTrue(AccountValidationError.INVALID_LAST_FOUR in AccountValidator.validate("Tarjeta", 0L, "123", emptySet()))
        assertTrue(AccountValidationError.INVALID_LAST_FOUR in AccountValidator.validate("Tarjeta", 0L, "12a4", emptySet()))
        assertTrue(AccountValidator.validate("Tarjeta", 0L, "1234", emptySet()).isEmpty())
        assertTrue(AccountValidator.validate("Tarjeta", 0L, "", emptySet()).isEmpty())
    }

    @Test
    fun validAccountHasNoErrors() {
        assertEquals(emptySet<AccountValidationError>(), AccountValidator.validate("Nueva", 1500L, "4321", setOf("efectivo")))
    }
}
