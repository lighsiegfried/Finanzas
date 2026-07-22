package com.kratt.finanzas.data.repository

import com.kratt.finanzas.data.local.entity.InstallmentOccurrenceEntity
import com.kratt.finanzas.data.local.entity.InstallmentPlanEntity
import com.kratt.finanzas.data.local.entity.RecurringOccurrenceEntity
import com.kratt.finanzas.data.local.entity.RecurringTemplateEntity
import com.kratt.finanzas.domain.model.InstallmentOccurrence
import com.kratt.finanzas.domain.model.InstallmentPlan
import com.kratt.finanzas.domain.model.RecurringOccurrence
import com.kratt.finanzas.domain.model.RecurringTemplate
import java.time.LocalDate

// traducciones entre las tablas de cuotas y recurrentes y sus modelos de dominio

fun InstallmentPlanEntity.toDomain(): InstallmentPlan = InstallmentPlan(
    id = id, name = name, accountId = accountId, categoryId = categoryId, totalAmountCents = totalAmountCents,
    installmentCount = installmentCount, installmentAmountCents = installmentAmountCents,
    firstDueDate = LocalDate.ofEpochDay(firstDueDate), frequency = frequency, paidInstallments = paidInstallments,
    status = status, description = description, createdAtMillis = createdAt, updatedAtMillis = updatedAt,
)

fun InstallmentOccurrenceEntity.toDomain(): InstallmentOccurrence = InstallmentOccurrence(
    id = id, installmentPlanId = installmentPlanId, sequenceNumber = sequenceNumber,
    dueDate = LocalDate.ofEpochDay(dueDate), amountCents = amountCents, status = status,
    paidTransactionId = paidTransactionId, paidAtMillis = paidAt, createdAtMillis = createdAt, updatedAtMillis = updatedAt,
)

fun RecurringTemplateEntity.toDomain(): RecurringTemplate = RecurringTemplate(
    id = id, name = name, transactionType = transactionType, accountId = accountId, categoryId = categoryId,
    amountCents = amountCents, recurrenceType = recurrenceType, interval = interval,
    startDate = LocalDate.ofEpochDay(startDate), endDate = endDate?.let(LocalDate::ofEpochDay),
    nextOccurrenceDate = LocalDate.ofEpochDay(nextOccurrenceDate), postingMode = postingMode, isActive = isActive,
    description = description, createdAtMillis = createdAt, updatedAtMillis = updatedAt,
)

fun RecurringOccurrenceEntity.toDomain(): RecurringOccurrence = RecurringOccurrence(
    id = id, recurringTemplateId = recurringTemplateId, scheduledDate = LocalDate.ofEpochDay(scheduledDate),
    amountCents = amountCents, status = status, generatedTransactionId = generatedTransactionId,
    createdAtMillis = createdAt, updatedAtMillis = updatedAt,
)
