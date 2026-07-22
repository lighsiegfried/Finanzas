package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.AttachmentStorageSummary
import com.kratt.finanzas.domain.repository.AttachmentRepository
import kotlinx.coroutines.flow.Flow

// resumen del espacio usado por los adjuntos, calculado desde los metadatos
class AttachmentStorageSummaryUseCase(
    private val repository: AttachmentRepository,
) {
    operator fun invoke(): Flow<AttachmentStorageSummary> = repository.observeStorageSummary()
}
