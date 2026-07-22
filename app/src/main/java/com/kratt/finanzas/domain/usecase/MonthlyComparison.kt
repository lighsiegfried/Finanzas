package com.kratt.finanzas.domain.usecase

import kotlin.math.abs

enum class ChangeDirection { UP, DOWN, SAME }

data class MetricChange(
    val previousCents: Long,
    val currentCents: Long,
    val deltaCents: Long,
    val direction: ChangeDirection,
    val hasPrevious: Boolean,
    val percentAbs: Int?,
)

// compara el mes actual con el anterior sin porcentajes engañosos ni division por cero
object MonthlyComparison {

    fun compare(previousCents: Long, currentCents: Long): MetricChange {
        val delta = currentCents - previousCents
        val direction = when {
            delta > 0 -> ChangeDirection.UP
            delta < 0 -> ChangeDirection.DOWN
            else -> ChangeDirection.SAME
        }
        val hasPrevious = previousCents != 0L
        // sin valor previo no se calcula porcentaje, se muestra el cambio absoluto
        val percent = if (hasPrevious) ((abs(delta) * 100) / abs(previousCents)).toInt() else null
        return MetricChange(previousCents, currentCents, delta, direction, hasPrevious, percent)
    }
}
