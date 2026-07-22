package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.ocr.OcrEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RunOcrUseCaseTest {

    private class FakeEngine(val available: Boolean, val text: String?) : OcrEngine {
        override fun isAvailable(): Boolean = available
        override suspend fun recognize(imageBytes: ByteArray): String? = text
    }

    @Test
    fun unavailableEngineReturnsUnavailable() = runTest {
        val result = RunOcrUseCase(FakeEngine(available = false, text = "Total Q 5.00")).run(ByteArray(1))
        assertTrue(result is OcrResult.Unavailable)
    }

    @Test
    fun noTextReturnsNoData() = runTest {
        val result = RunOcrUseCase(FakeEngine(available = true, text = null)).run(ByteArray(1))
        assertTrue(result is OcrResult.NoData)
    }

    @Test
    fun unrecognizableTextReturnsNoData() = runTest {
        val result = RunOcrUseCase(FakeEngine(available = true, text = "    ")).run(ByteArray(1))
        assertTrue(result is OcrResult.NoData)
    }

    @Test
    fun recognizedReceiptReturnsDetected() = runTest {
        val result = RunOcrUseCase(FakeEngine(available = true, text = "Tienda\nTotal Q 25.75")).run(ByteArray(1))
        assertTrue(result is OcrResult.Detected)
        assertTrue((result as OcrResult.Detected).suggestions.totalCents == 2575L)
    }
}
