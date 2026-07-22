package com.kratt.finanzas.presentation.common

import androidx.annotation.StringRes
import com.kratt.finanzas.R
import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus
import com.kratt.finanzas.domain.model.RecurringOccurrenceStatus

// etiquetas visibles de los estados, para no depender solo del color
object StatusLabels {

    @StringRes
    fun installmentOccurrence(status: InstallmentOccurrenceStatus): Int = when (status) {
        InstallmentOccurrenceStatus.PENDING -> R.string.status_pending
        InstallmentOccurrenceStatus.PAID -> R.string.status_paid
        InstallmentOccurrenceStatus.OVERDUE -> R.string.status_overdue
        InstallmentOccurrenceStatus.SKIPPED -> R.string.status_skipped
        InstallmentOccurrenceStatus.CANCELLED -> R.string.status_skipped
    }

    @StringRes
    fun recurringOccurrence(status: RecurringOccurrenceStatus): Int = when (status) {
        RecurringOccurrenceStatus.PENDING -> R.string.status_pending
        RecurringOccurrenceStatus.POSTED -> R.string.status_paid
        RecurringOccurrenceStatus.OVERDUE -> R.string.status_overdue
        RecurringOccurrenceStatus.SKIPPED -> R.string.status_skipped
        RecurringOccurrenceStatus.CANCELLED -> R.string.status_skipped
    }
}
