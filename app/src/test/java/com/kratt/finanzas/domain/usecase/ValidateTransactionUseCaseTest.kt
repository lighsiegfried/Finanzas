package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.TransactionDraft
import com.kratt.finanzas.domain.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateTransactionUseCaseTest {

    private val validate = ValidateTransactionUseCase()

    // borrador valido base que cada prueba modifica segun lo que quiere romper
    private fun draft(
        amountCents: Long? = 12_575,
        accountId: Long? = 1L,
        categoryId: Long? = 2L,
        date: LocalDate? = LocalDate.of(2026, 7, 19),
    ): TransactionDraft = TransactionDraft(
        type = TransactionType.EXPENSE,
        amountCents = amountCents,
        accountId = accountId,
        categoryId = categoryId,
        description = "Compra de prueba",
        date = date,
    )

    @Test
    fun validDraft_hasNoErrors() {
        assertTrue(validate(draft()).isEmpty())
    }

    @Test
    fun zeroAmount_reportsInvalidAmount() {
        assertTrue(TransactionValidationError.INVALID_AMOUNT in validate(draft(amountCents = 0)))
    }

    @Test
    fun nullAmount_reportsInvalidAmount() {
        assertTrue(TransactionValidationError.INVALID_AMOUNT in validate(draft(amountCents = null)))
    }

    @Test
    fun negativeAmount_reportsInvalidAmount() {
        assertTrue(TransactionValidationError.INVALID_AMOUNT in validate(draft(amountCents = -100)))
    }

    @Test
    fun missingAccount_reportsMissingAccount() {
        assertTrue(TransactionValidationError.MISSING_ACCOUNT in validate(draft(accountId = null)))
    }

    @Test
    fun missingCategory_reportsMissingCategory() {
        assertTrue(TransactionValidationError.MISSING_CATEGORY in validate(draft(categoryId = null)))
    }

    @Test
    fun missingDate_reportsInvalidDate() {
        assertTrue(TransactionValidationError.INVALID_DATE in validate(draft(date = null)))
    }

    @Test
    fun emptyDraft_reportsAllErrorsTogether() {
        val errors = validate(
            draft(amountCents = null, accountId = null, categoryId = null, date = null),
        )
        assertEquals(4, errors.size)
    }
}
