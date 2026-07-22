package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.repository.AttachmentRepository

// elimina el metadato y el archivo cifrado sin tocar el movimiento financiero
class DeleteAttachmentUseCase(
    private val repository: AttachmentRepository,
) {
    suspend fun delete(attachmentId: Long): Boolean = repository.delete(attachmentId)
}
