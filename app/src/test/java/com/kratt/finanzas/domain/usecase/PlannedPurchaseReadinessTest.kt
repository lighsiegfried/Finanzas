package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.PurchaseStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PlannedPurchaseReadinessTest {

    @Test
    fun notFunded_whenNoSavings() {
        val r = PlannedPurchaseReadinessCalculator.calculate(PurchaseStatus.PLANNING, 800_000, 0)
        assertEquals(PurchaseReadiness.NOT_FUNDED, r.readiness)
        assertEquals(800_000, r.remainingToPurchaseCents)
    }

    @Test
    fun partiallyFunded() {
        val r = PlannedPurchaseReadinessCalculator.calculate(PurchaseStatus.SAVING, 800_000, 300_000)
        assertEquals(PurchaseReadiness.PARTIALLY_FUNDED, r.readiness)
        assertEquals(500_000, r.remainingToPurchaseCents)
    }

    @Test
    fun ready_whenSavedMeetsCost() {
        val r = PlannedPurchaseReadinessCalculator.calculate(PurchaseStatus.READY, 800_000, 800_000)
        assertEquals(PurchaseReadiness.READY, r.readiness)
        assertEquals(0, r.remainingToPurchaseCents)
    }

    @Test
    fun ready_whenSavedExceedsCost_remainingNeverNegative() {
        val r = PlannedPurchaseReadinessCalculator.calculate(PurchaseStatus.READY, 800_000, 900_000)
        assertEquals(PurchaseReadiness.READY, r.readiness)
        assertEquals(0, r.remainingToPurchaseCents)
    }

    @Test
    fun purchased_status() {
        val r = PlannedPurchaseReadinessCalculator.calculate(PurchaseStatus.PURCHASED, 800_000, 800_000)
        assertEquals(PurchaseReadiness.PURCHASED, r.readiness)
    }

    @Test
    fun nullLinkedGoal_treatedAsNotFunded() {
        val r = PlannedPurchaseReadinessCalculator.calculate(PurchaseStatus.PLANNING, 800_000, null)
        assertEquals(PurchaseReadiness.NOT_FUNDED, r.readiness)
        assertEquals(0, r.availableSavedCents)
    }
}
