package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.RecurringOccurrenceStatus

// solo una ocurrencia pendiente puede registrarse, asi el registro pasa una sola vez
object RecurringPostingRules {
    fun canPost(status: RecurringOccurrenceStatus): Boolean =
        status == RecurringOccurrenceStatus.PENDING
}
