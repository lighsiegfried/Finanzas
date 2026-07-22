package com.kratt.finanzas.domain.usecase

import org.junit.Assert.assertTrue
import org.junit.Test

class TransferValidatorTest {

    @Test
    fun missingAccountsRejected() {
        val errors = TransferValidator.validate(null, null, 1000L)
        assertTrue(TransferValidationError.MISSING_SOURCE in errors)
        assertTrue(TransferValidationError.MISSING_DESTINATION in errors)
    }

    @Test
    fun sameAccountRejected() {
        assertTrue(TransferValidationError.SAME_ACCOUNT in TransferValidator.validate(5L, 5L, 1000L))
    }

    @Test
    fun invalidAmountRejected() {
        assertTrue(TransferValidationError.INVALID_AMOUNT in TransferValidator.validate(1L, 2L, 0L))
        assertTrue(TransferValidationError.INVALID_AMOUNT in TransferValidator.validate(1L, 2L, null))
    }

    @Test
    fun validTransferHasNoErrors() {
        assertTrue(TransferValidator.validate(1L, 2L, 2500L).isEmpty())
    }
}
