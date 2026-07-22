package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.PlannedPurchase
import com.kratt.finanzas.domain.model.SavingsGoal

// errores de validacion de una meta
enum class SavingsGoalValidationError {
    NAME_REQUIRED,
    INVALID_TARGET,
    TARGET_DATE_BEFORE_START,
    AMOUNT_TOO_LARGE,
}

object SavingsGoalValidator {
    fun validate(goal: SavingsGoal): Set<SavingsGoalValidationError> {
        val errors = mutableSetOf<SavingsGoalValidationError>()
        if (goal.name.isBlank()) errors += SavingsGoalValidationError.NAME_REQUIRED
        if (goal.targetAmountCents <= 0L) errors += SavingsGoalValidationError.INVALID_TARGET
        else if (goal.targetAmountCents > MoneyMath.MAX_SUPPORTED_CENTS) errors += SavingsGoalValidationError.AMOUNT_TOO_LARGE
        // la fecha objetivo debe ser posterior a la fecha inicial
        val target = goal.targetDate
        if (target != null && !target.isAfter(goal.startDate)) errors += SavingsGoalValidationError.TARGET_DATE_BEFORE_START
        return errors
    }
}

// errores de validacion de una compra planificada
enum class PurchaseValidationError {
    NAME_REQUIRED,
    INVALID_COST,
    AMOUNT_TOO_LARGE,
}

object PlannedPurchaseValidator {
    fun validate(purchase: PlannedPurchase): Set<PurchaseValidationError> {
        val errors = mutableSetOf<PurchaseValidationError>()
        if (purchase.name.isBlank()) errors += PurchaseValidationError.NAME_REQUIRED
        if (purchase.estimatedCostCents <= 0L) errors += PurchaseValidationError.INVALID_COST
        else if (purchase.estimatedCostCents > MoneyMath.MAX_SUPPORTED_CENTS) errors += PurchaseValidationError.AMOUNT_TOO_LARGE
        return errors
    }
}

// errores de validacion de un aporte
enum class ContributionValidationError {
    INVALID_AMOUNT,
    AMOUNT_TOO_LARGE,
    SOURCE_ACCOUNT_REQUIRED,
}
