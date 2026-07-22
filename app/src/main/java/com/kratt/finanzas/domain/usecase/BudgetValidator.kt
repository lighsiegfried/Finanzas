package com.kratt.finanzas.domain.usecase

enum class BudgetValidationError {
    INVALID_AMOUNT,
    MISSING_CATEGORY,
    DUPLICATE,
    INVALID_WARNING,
}

object BudgetValidator {

    const val DEFAULT_WARNING = 80
    const val MIN_WARNING = 1
    const val MAX_WARNING = 100

    // valida el monto, la categoria cuando aplica, el aviso y que no exista ya
    fun validate(
        limitCents: Long?,
        isCategoryBudget: Boolean,
        categoryId: Long?,
        warningPercentage: Int,
        alreadyExists: Boolean,
    ): Set<BudgetValidationError> = buildSet {
        if (limitCents == null || limitCents <= 0L) add(BudgetValidationError.INVALID_AMOUNT)
        if (isCategoryBudget && categoryId == null) add(BudgetValidationError.MISSING_CATEGORY)
        if (warningPercentage < MIN_WARNING || warningPercentage > MAX_WARNING) add(BudgetValidationError.INVALID_WARNING)
        if (alreadyExists) add(BudgetValidationError.DUPLICATE)
    }
}
