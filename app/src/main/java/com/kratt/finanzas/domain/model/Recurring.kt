package com.kratt.finanzas.domain.model

import java.time.LocalDate

// plantilla recurrente en el dominio
data class RecurringTemplate(
    val id: Long,
    val name: String,
    val transactionType: TransactionType,
    val accountId: Long,
    val categoryId: Long,
    val amountCents: Long,
    val recurrenceType: RecurrenceType,
    val interval: Int,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val nextOccurrenceDate: LocalDate,
    val postingMode: PostingMode,
    val isActive: Boolean,
    val description: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)

data class RecurringOccurrence(
    val id: Long,
    val recurringTemplateId: Long,
    val scheduledDate: LocalDate,
    val amountCents: Long,
    val status: RecurringOccurrenceStatus,
    val generatedTransactionId: Long? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)
