package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.AttachmentType
import com.kratt.finanzas.domain.repository.AttachmentRepository
import java.io.InputStream

// resultado de adjuntar un archivo a un movimiento
sealed interface AddAttachmentResult {
    data class Success(val attachmentId: Long) : AddAttachmentResult
    data class Failure(val error: AttachmentError) : AddAttachmentResult
}

// valida el archivo y lo adjunta al movimiento; nunca deja metadatos ni archivos a medias
class AddAttachmentUseCase(
    private val repository: AttachmentRepository,
) {
    suspend fun add(
        transactionId: Long,
        rawDisplayName: String,
        mimeType: String,
        requestedType: AttachmentType,
        input: InputStream,
    ): AddAttachmentResult {
        val type = AttachmentValidator.resolveType(requestedType, mimeType)
            ?: return AddAttachmentResult.Failure(AttachmentError.UNSUPPORTED_TYPE)
        val name = AttachmentValidator.sanitizeDisplayName(rawDisplayName)
        return repository.add(transactionId, name, mimeType, type, input)
    }
}
