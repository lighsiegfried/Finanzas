package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.Attachment
import com.kratt.finanzas.domain.repository.AttachmentRepository
import kotlinx.coroutines.flow.Flow

// observa los adjuntos de un movimiento y su cantidad
class ObserveAttachmentsUseCase(
    private val repository: AttachmentRepository,
) {
    operator fun invoke(transactionId: Long): Flow<List<Attachment>> =
        repository.observeForTransaction(transactionId)

    fun count(transactionId: Long): Flow<Int> =
        repository.observeCountForTransaction(transactionId)
}
