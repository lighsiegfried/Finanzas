package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.AccountTotals
import com.kratt.finanzas.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountBalanceCalculatorTest {

    private fun account(type: AccountType, initial: Long = 0L, limit: Long? = null) = Account(
        id = 1L,
        name = "Cuenta",
        type = type,
        currencyCode = "GTQ",
        initialBalanceCents = initial,
        isActive = true,
        creditLimitCents = limit,
    )

    @Test
    fun normalBalanceUsesAllMovementDirections() {
        val totals = AccountTotals(incomeCents = 5000, expenseCents = 2000, transferInCents = 1000, transferOutCents = 500)
        val balance = AccountBalanceCalculator.calculate(account(AccountType.CASH, initial = 10_000), totals)
        assertEquals(13_500L, balance.currentBalanceCents)
        assertFalse(balance.isCredit)
        assertNull(balance.availableCreditCents)
    }

    @Test
    fun transfersMoveBalanceButAreDirectional() {
        val onlyOut = AccountTotals(transferOutCents = 4000)
        val onlyIn = AccountTotals(transferInCents = 4000)
        assertEquals(-4000L, AccountBalanceCalculator.calculate(account(AccountType.CASH), onlyOut).currentBalanceCents)
        assertEquals(4000L, AccountBalanceCalculator.calculate(account(AccountType.CASH), onlyIn).currentBalanceCents)
    }

    @Test
    fun creditCardDebtAndAvailable() {
        val totals = AccountTotals(expenseCents = 30_000, transferInCents = 10_000)
        val balance = AccountBalanceCalculator.calculate(
            account(AccountType.CREDIT_CARD, initial = 0, limit = 100_000),
            totals,
        )
        assertTrue(balance.isCredit)
        assertEquals(20_000L, balance.debtCents)
        assertTrue(balance.hasCreditLimit)
        assertEquals(80_000L, balance.availableCreditCents)
    }

    @Test
    fun creditCardWithoutLimitHasNoAvailable() {
        val totals = AccountTotals(expenseCents = 30_000)
        val balance = AccountBalanceCalculator.calculate(account(AccountType.CREDIT_CARD, limit = null), totals)
        assertTrue(balance.isCredit)
        assertEquals(30_000L, balance.debtCents)
        assertFalse(balance.hasCreditLimit)
        assertNull(balance.availableCreditCents)
    }
}
