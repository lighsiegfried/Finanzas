package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.AttachmentType

// razones de fallo al adjuntar; el viewmodel las traduce a mensajes en espanol
enum class AttachmentError {
    UNSUPPORTED_TYPE,
    TOO_LARGE,
    EMPTY,
    UNREADABLE,
    NO_SPACE,
    GENERIC,
}

// reglas puras de validacion de adjuntos, sin depender de android para poder probarlas
object AttachmentValidator {

    // limite por archivo, evita respaldos y almacenamiento demasiado grandes
    const val MAX_BYTES: Long = 20L * 1024 * 1024
    private const val MAX_NAME_LENGTH = 120

    // solo se aceptan imagenes y pdf; nunca ejecutables ni otros formatos
    fun isSupportedMime(mimeType: String): Boolean {
        val m = mimeType.trim().lowercase()
        return m.startsWith("image/") || m == "application/pdf"
    }

    // valida el mime declarado y el tamano conocido; devuelve null si todo esta bien
    fun validate(mimeType: String, sizeBytes: Long?): AttachmentError? {
        if (!isSupportedMime(mimeType)) return AttachmentError.UNSUPPORTED_TYPE
        if (sizeBytes != null) {
            if (sizeBytes <= 0L) return AttachmentError.EMPTY
            if (sizeBytes > MAX_BYTES) return AttachmentError.TOO_LARGE
        }
        return null
    }

    // decide el tipo final a partir del mime y del origen pedido; null si no es soportado
    fun resolveType(requested: AttachmentType, mimeType: String): AttachmentType? {
        val m = mimeType.trim().lowercase()
        return when {
            m == "application/pdf" -> AttachmentType.DOCUMENT_PDF
            m.startsWith("image/") -> when (requested) {
                AttachmentType.RECEIPT_IMAGE, AttachmentType.SUPPORT_IMAGE -> requested
                else -> AttachmentType.SUPPORT_IMAGE
            }
            else -> null
        }
    }

    // las imagenes muestran miniatura y los pdf se pueden previsualizar por pagina
    fun previewAvailable(mimeType: String): Boolean = isSupportedMime(mimeType)

    // limpia el nombre visible: quita rutas, caracteres de control y limita el largo
    fun sanitizeDisplayName(raw: String, fallback: String = "adjunto"): String {
        val base = raw.substringAfterLast('/').substringAfterLast('\\').trim()
        val cleaned = base.filter { it.code >= 0x20 && it.code != 0x7f }.trim()
        val safe = if (cleaned.isBlank()) fallback else cleaned
        return if (safe.length > MAX_NAME_LENGTH) safe.take(MAX_NAME_LENGTH) else safe
    }

    // suma tamanos sin desbordar; util para el resumen de almacenamiento
    fun safeAddSize(a: Long, b: Long): Long {
        require(a >= 0 && b >= 0) { "negative size" }
        return Math.addExact(a, b)
    }

    fun sumSizes(sizes: List<Long>): Long = sizes.fold(0L) { acc, s -> safeAddSize(acc, s) }
}
