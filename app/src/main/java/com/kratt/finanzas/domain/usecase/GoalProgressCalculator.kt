package com.kratt.finanzas.domain.usecase

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// avance de una meta calculado de forma determinista, siempre con enteros
data class GoalProgress(
    val contributedCents: Long,
    val remainingCents: Long,
    val surplusCents: Long,
    val progressPercent: Int,
    val isComplete: Boolean,
    val hasTargetDate: Boolean,
    // sugerencia mensual solo si hay fecha objetivo y falta dinero
    val suggestedMonthlyCents: Long?,
    // promedio mensual de aportes segun el historial
    val averageMonthlyCents: Long?,
    // fecha estimada segun el promedio; null si no hay historial suficiente
    val estimatedDate: LocalDate?,
)

// un aporte visto por el calculo: solo monto y fecha
data class ContributionPoint(val amountCents: Long, val date: LocalDate)

object GoalProgressCalculator {

    fun calculate(
        targetAmountCents: Long,
        contributions: List<ContributionPoint>,
        startDate: LocalDate,
        targetDate: LocalDate?,
        today: LocalDate = LocalDate.now(),
    ): GoalProgress {
        // suma segura de aportes, detecta desbordes
        val contributed = MoneyMath.sum(contributions.map { it.amountCents })
        val isComplete = contributed >= targetAmountCents
        // el faltante nunca se muestra negativo como dinero pendiente
        val remaining = if (contributed >= targetAmountCents) 0L else targetAmountCents - contributed
        val surplus = if (contributed > targetAmountCents) contributed - targetAmountCents else 0L
        val percent = if (targetAmountCents > 0) ((contributed * 100) / targetAmountCents).toInt() else 0

        // promedio mensual sobre los meses transcurridos desde el inicio, minimo un mes
        val monthsElapsed = maxOf(1L, ChronoUnit.MONTHS.between(YearMonth.from(startDate), YearMonth.from(today)) + 1)
        val average = if (contributed > 0) contributed / monthsElapsed else null

        // sugerencia mensual solo con fecha objetivo y saldo pendiente
        val suggested = if (targetDate != null && !isComplete) {
            val months = maxOf(1L, ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(targetDate)))
            ceilDiv(remaining, months)
        } else {
            null
        }

        // fecha estimada segun el promedio; sin historial util no se inventa
        val estimated = if (!isComplete && average != null && average > 0) {
            val monthsNeeded = ceilDiv(remaining, average)
            today.plusMonths(monthsNeeded)
        } else {
            null
        }

        return GoalProgress(
            contributedCents = contributed,
            remainingCents = remaining,
            surplusCents = surplus,
            progressPercent = percent,
            isComplete = isComplete,
            hasTargetDate = targetDate != null,
            suggestedMonthlyCents = suggested,
            averageMonthlyCents = average,
            estimatedDate = estimated,
        )
    }

    // division hacia arriba para no quedar corto en la sugerencia; nunca pierde centavos
    private fun ceilDiv(value: Long, divisor: Long): Long =
        if (divisor <= 0L) value else (value + divisor - 1) / divisor
}
