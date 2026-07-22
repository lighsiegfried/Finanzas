package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.PurchaseStatus

// estado de avance de una compra planificada
enum class PurchaseReadiness { NOT_FUNDED, PARTIALLY_FUNDED, READY, PURCHASED }

data class PurchaseReadinessResult(
    val readiness: PurchaseReadiness,
    val availableSavedCents: Long,
    val remainingToPurchaseCents: Long,
)

// calcula la disponibilidad de una compra a partir de la meta ligada, sin doble asignacion
object PlannedPurchaseReadinessCalculator {

    fun calculate(
        status: PurchaseStatus,
        estimatedCostCents: Long,
        linkedGoalContributedCents: Long?,
    ): PurchaseReadinessResult {
        if (status == PurchaseStatus.PURCHASED) {
            return PurchaseReadinessResult(PurchaseReadiness.PURCHASED, linkedGoalContributedCents ?: 0L, 0L)
        }
        // solo se cuenta lo ahorrado en la meta explicitamente ligada
        val available = linkedGoalContributedCents ?: 0L
        val remaining = if (available >= estimatedCostCents) 0L else estimatedCostCents - available
        val readiness = when {
            available <= 0L -> PurchaseReadiness.NOT_FUNDED
            available >= estimatedCostCents -> PurchaseReadiness.READY
            else -> PurchaseReadiness.PARTIALLY_FUNDED
        }
        return PurchaseReadinessResult(readiness, available, remaining)
    }
}
