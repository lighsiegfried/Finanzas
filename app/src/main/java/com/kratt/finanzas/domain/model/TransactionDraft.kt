package com.kratt.finanzas.domain.model

import java.time.LocalDate

// borrador del formulario antes de validarlo y guardarlo
data class TransactionDraft(
    val type: TransactionType,
    val amountCents: Long?,
    val accountId: Long?,
    val categoryId: Long?,
    val description: String?,
    val date: LocalDate?,
)
