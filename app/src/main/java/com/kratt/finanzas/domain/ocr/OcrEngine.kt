package com.kratt.finanzas.domain.ocr

// motor de ocr local; una implementacion real corre tesseract en el dispositivo, sin red ni servicios
// se mantiene detras de esta interfaz para que el resto de la app no dependa del motor concreto
interface OcrEngine {

    fun isAvailable(): Boolean

    // devuelve el texto reconocido o null si el motor no esta disponible o no reconoce nada
    suspend fun recognize(imageBytes: ByteArray): String?
}
