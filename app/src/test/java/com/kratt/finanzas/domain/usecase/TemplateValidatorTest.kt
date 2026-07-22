package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.TransactionTemplate
import com.kratt.finanzas.domain.model.TransactionType
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateValidatorTest {

    private fun expense(name: String = "Gasolina", account: Long = 1, category: Long? = 2, amount: Long? = null) =
        TransactionTemplate(name = name, type = TransactionType.EXPENSE, accountId = account, categoryId = category, defaultAmountCents = amount)

    @Test
    fun validExpense_hasNoErrors() {
        assertTrue(TemplateValidator.validate(expense()).isEmpty())
    }

    @Test
    fun blankName() {
        assertTrue(TemplateValidationError.NAME_REQUIRED in TemplateValidator.validate(expense(name = " ")))
    }

    @Test
    fun missingAccount() {
        assertTrue(TemplateValidationError.ACCOUNT_REQUIRED in TemplateValidator.validate(expense(account = 0)))
    }

    @Test
    fun expenseMissingCategory() {
        assertTrue(TemplateValidationError.CATEGORY_REQUIRED in TemplateValidator.validate(expense(category = null)))
    }

    @Test
    fun transferSameAccounts() {
        val t = TransactionTemplate(name = "Ahorro", type = TransactionType.TRANSFER, accountId = 1, destinationAccountId = 1)
        assertTrue(TemplateValidationError.SAME_ACCOUNTS in TemplateValidator.validate(t))
    }

    @Test
    fun transferMissingDestination() {
        val t = TransactionTemplate(name = "Ahorro", type = TransactionType.TRANSFER, accountId = 1, destinationAccountId = null)
        assertTrue(TemplateValidationError.ACCOUNT_REQUIRED in TemplateValidator.validate(t))
    }

    @Test
    fun validTransfer_hasNoErrors() {
        val t = TransactionTemplate(name = "Ahorro", type = TransactionType.TRANSFER, accountId = 1, destinationAccountId = 2)
        assertTrue(TemplateValidator.validate(t).isEmpty())
    }

    @Test
    fun invalidAmountRejected_butEmptyAmountAllowed() {
        assertTrue(TemplateValidationError.INVALID_AMOUNT in TemplateValidator.validate(expense(amount = 0)))
        assertTrue(TemplateValidator.validate(expense(amount = null)).isEmpty())
    }
}
