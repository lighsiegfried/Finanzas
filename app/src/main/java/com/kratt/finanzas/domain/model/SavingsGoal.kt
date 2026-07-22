package com.kratt.finanzas.domain.model

import java.time.LocalDate

// meta de ahorro; el avance real se calcula desde los aportes
data class SavingsGoal(
    val id: Long = 0L,
    val name: String,
    val targetAmountCents: Long,
    val linkedAccountId: Long? = null,
    val startDate: LocalDate,
    val targetDate: LocalDate? = null,
    val status: SavingsGoalStatus = SavingsGoalStatus.ACTIVE,
    val description: String? = null,
    val iconKey: String = "savings",
    val colorKey: String = "green",
    val isArchived: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

// aporte a una meta
data class SavingsContribution(
    val id: Long = 0L,
    val savingsGoalId: Long,
    val amountCents: Long,
    val contributionDate: LocalDate,
    val sourceAccountId: Long? = null,
    val linkedTransactionId: Long? = null,
    val contributionType: ContributionType,
    val note: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

// compra planificada; no genera gasto hasta registrarse
data class PlannedPurchase(
    val id: Long = 0L,
    val name: String,
    val estimatedCostCents: Long,
    val categoryId: Long? = null,
    val savingsGoalId: Long? = null,
    val targetDate: LocalDate? = null,
    val priority: PurchasePriority = PurchasePriority.MEDIUM,
    val status: PurchaseStatus = PurchaseStatus.PLANNING,
    val description: String? = null,
    val vendor: String? = null,
    val purchasedTransactionId: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
