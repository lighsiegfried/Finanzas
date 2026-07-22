package com.kratt.finanzas.domain.model

import java.time.LocalDate

// compra en cuotas en el dominio, las fechas son localdate
data class InstallmentPlan(
    val id: Long,
    val name: String,
    val accountId: Long,
    val categoryId: Long,
    val totalAmountCents: Long,
    val installmentCount: Int,
    val installmentAmountCents: Long,
    val firstDueDate: LocalDate,
    val frequency: InstallmentFrequency,
    val paidInstallments: Int,
    val status: InstallmentStatus,
    val description: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)

data class InstallmentOccurrence(
    val id: Long,
    val installmentPlanId: Long,
    val sequenceNumber: Int,
    val dueDate: LocalDate,
    val amountCents: Long,
    val status: InstallmentOccurrenceStatus,
    val paidTransactionId: Long? = null,
    val paidAtMillis: Long? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)
