package com.kratt.finanzas.domain.model

// tipo de adjunto de un movimiento; se guarda por el nombre del enum
enum class AttachmentType {
    RECEIPT_IMAGE,
    DOCUMENT_PDF,
    SUPPORT_IMAGE,
    OTHER_DOCUMENT,
}

// adjunto que pertenece a un movimiento; el archivo real vive cifrado en almacenamiento privado
data class Attachment(
    val id: Long,
    val transactionId: Long,
    val displayName: String,
    val mimeType: String,
    val storedFileName: String,
    val sizeBytes: Long,
    val checksum: String,
    val attachmentType: AttachmentType,
    val previewAvailable: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
) {
    // solo las imagenes tienen miniatura; los pdf se muestran como documento
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isPdf: Boolean get() = mimeType == "application/pdf"
}

// resumen del espacio usado por los adjuntos, calculado desde los metadatos
data class AttachmentStorageSummary(
    val fileCount: Int,
    val totalBytes: Long,
)
