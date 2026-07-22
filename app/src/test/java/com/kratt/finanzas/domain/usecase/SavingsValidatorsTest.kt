package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.PlannedPurchase
import com.kratt.finanzas.domain.model.SavingsGoal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsValidatorsTest {

    private val start = LocalDate.of(2026, 1, 1)
    private fun goal(name: String = "Meta", target: Long = 500_000, targetDate: LocalDate? = null) =
        SavingsGoal(name = name, targetAmountCents = target, startDate = start, targetDate = targetDate)

    @Test
    fun validGoal_hasNoErrors() {
        assertTrue(SavingsGoalValidator.validate(goal(targetDate = LocalDate.of(2026, 6, 1))).isEmpty())
    }

    @Test
    fun blankName() {
        assertTrue(SavingsGoalValidationError.NAME_REQUIRED in SavingsGoalValidator.validate(goal(name = "  ")))
    }

    @Test
    fun invalidTarget() {
        assertTrue(SavingsGoalValidationError.INVALID_TARGET in SavingsGoalValidator.validate(goal(target = 0)))
    }

    @Test
    fun targetTooLarge() {
        assertTrue(SavingsGoalValidationError.AMOUNT_TOO_LARGE in SavingsGoalValidator.validate(goal(target = MoneyMath.MAX_SUPPORTED_CENTS + 1)))
    }

    @Test
    fun targetDateBeforeStart() {
        val e = SavingsGoalValidator.validate(goal(targetDate = LocalDate.of(2025, 12, 1)))
        assertTrue(SavingsGoalValidationError.TARGET_DATE_BEFORE_START in e)
    }

    @Test
    fun purchaseValidation() {
        assertTrue(PlannedPurchaseValidator.validate(PlannedPurchase(name = "  ", estimatedCostCents = 100)).contains(PurchaseValidationError.NAME_REQUIRED))
        assertTrue(PlannedPurchaseValidator.validate(PlannedPurchase(name = "Laptop", estimatedCostCents = 0)).contains(PurchaseValidationError.INVALID_COST))
        assertTrue(PlannedPurchaseValidator.validate(PlannedPurchase(name = "Laptop", estimatedCostCents = MoneyMath.MAX_SUPPORTED_CENTS + 1)).contains(PurchaseValidationError.AMOUNT_TOO_LARGE))
        assertEquals(emptySet<PurchaseValidationError>(), PlannedPurchaseValidator.validate(PlannedPurchase(name = "Laptop", estimatedCostCents = 800_000)))
    }
}
