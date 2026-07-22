package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.ocr.OcrEngine

// resultado de leer un comprobante; solo propone datos, nunca guarda ni modifica un movimiento
sealed interface OcrResult {
    data class Detected(val suggestions: OcrSuggestions) : OcrResult
    data object NoData : OcrResult
    data object Unavailable : OcrResult
}

// corre el ocr local sobre la imagen y convierte el texto en sugerencias para revisar
class RunOcrUseCase(
    private val engine: OcrEngine,
) {
    val available: Boolean get() = engine.isAvailable()

    suspend fun run(imageBytes: ByteArray): OcrResult {
        if (!engine.isAvailable()) return OcrResult.Unavailable
        val text = engine.recognize(imageBytes) ?: return OcrResult.NoData
        val suggestions = OcrReceiptParser.parse(text)
        return if (suggestions.hasAny) OcrResult.Detected(suggestions) else OcrResult.NoData
    }
}
