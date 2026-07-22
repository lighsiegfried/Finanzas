package com.kratt.finanzas.data.ocr

import com.kratt.finanzas.domain.ocr.OcrEngine

// motor por defecto sin dependencia nativa; reporta ocr no disponible
// el motor real (tesseract local, sin red) se conecta detras de la interfaz OcrEngine
class NoOpOcrEngine : OcrEngine {
    override fun isAvailable(): Boolean = false
    override suspend fun recognize(imageBytes: ByteArray): String? = null
}
