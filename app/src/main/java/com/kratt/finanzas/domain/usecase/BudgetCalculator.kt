package com.kratt.finanzas.domain.usecase

// estado de un presupuesto, no depende solo del color
enum class BudgetState { AVAILABLE, WARNING, EXCEEDED }

data class BudgetProgress(
    val limitCents: Long,
    val spentCents: Long,
    val remainingCents: Long,
    val percentage: Int,
    val state: BudgetState,
)

// calcula el avance de un presupuesto con enteros, el porcentaje es solo para mostrar
object BudgetCalculator {

    fun progress(limitCents: Long, spentCents: Long, warningPercentage: Int): BudgetProgress {
        // limit siempre es mayor a cero, aun asi se protege la division
        val percentage = if (limitCents > 0) ((spentCents * 100) / limitCents).toInt() else 0
        val state = when {
            spentCents > limitCents -> BudgetState.EXCEEDED
            percentage >= warningPercentage -> BudgetState.WARNING
            else -> BudgetState.AVAILABLE
        }
        return BudgetProgress(
            limitCents = limitCents,
            spentCents = spentCents,
            remainingCents = limitCents - spentCents,
            percentage = percentage,
            state = state,
        )
    }
}
