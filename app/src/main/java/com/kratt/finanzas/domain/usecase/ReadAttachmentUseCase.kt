package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.Attachment
import com.kratt.finanzas.domain.repository.AttachmentRepository
import java.io.File

// descifra el contenido de un adjunto solo cuando se necesita mostrarlo
class ReadAttachmentUseCase(
    private val repository: AttachmentRepository,
) {
    // bytes en memoria para una imagen
    suspend fun bytes(attachment: Attachment): ByteArray? = repository.readBytes(attachment)

    // archivo temporal de cache para abrir un pdf; el llamador lo borra al cerrar
    suspend fun toCacheFile(attachment: Attachment): File? = repository.decryptToCache(attachment)
}
