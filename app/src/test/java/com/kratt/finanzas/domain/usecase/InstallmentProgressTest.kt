package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallmentProgressTest {

    private fun occ(amount: Long, status: InstallmentOccurrenceStatus) = OccurrenceView(amount, status)

    @Test
    fun progressCountsPaidAndPending() {
        val progress = InstallmentProgressCalculator.calculate(
            listOf(
                occ(12_800, InstallmentOccurrenceStatus.PAID),
                occ(12_800, InstallmentOccurrenceStatus.PAID),
                occ(12_800, InstallmentOccurrenceStatus.PENDING),
                occ(12_800, InstallmentOccurrenceStatus.OVERDUE),
            ),
        )
        assertEquals(51_200L, progress.totalCents)
        assertEquals(25_600L, progress.paidCents)
        assertEquals(25_600L, progress.remainingCents)
        assertEquals(2, progress.paidCount)
        assertEquals(2, progress.pendingCount)
        assertFalse(progress.isComplete)
    }

    @Test
    fun completeWhenNoPendingLeft() {
        val progress = InstallmentProgressCalculator.calculate(
            listOf(occ(500, InstallmentOccurrenceStatus.PAID), occ(500, InstallmentOccurrenceStatus.PAID)),
        )
        assertTrue(progress.isComplete)
        assertEquals(0L, progress.remainingCents)
    }

    @Test
    fun canPayOnlyPendingOrOverdueWithoutTransaction() {
        assertTrue(InstallmentPaymentRules.canPay(InstallmentOccurrenceStatus.PENDING, null))
        assertTrue(InstallmentPaymentRules.canPay(InstallmentOccurrenceStatus.OVERDUE, null))
        assertFalse(InstallmentPaymentRules.canPay(InstallmentOccurrenceStatus.PENDING, 5L))
        assertFalse(InstallmentPaymentRules.canPay(InstallmentOccurrenceStatus.PAID, 5L))
    }

    @Test
    fun canRevertOnlyPaidWithTransaction() {
        assertTrue(InstallmentPaymentRules.canRevert(InstallmentOccurrenceStatus.PAID, 9L))
        assertFalse(InstallmentPaymentRules.canRevert(InstallmentOccurrenceStatus.PAID, null))
        assertFalse(InstallmentPaymentRules.canRevert(InstallmentOccurrenceStatus.PENDING, null))
    }
}
