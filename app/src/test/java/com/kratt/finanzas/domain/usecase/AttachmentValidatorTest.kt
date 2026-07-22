package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.AttachmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentValidatorTest {

    @Test
    fun supportedMimeAcceptsImagesAndPdfOnly() {
        assertTrue(AttachmentValidator.isSupportedMime("image/jpeg"))
        assertTrue(AttachmentValidator.isSupportedMime("image/png"))
        assertTrue(AttachmentValidator.isSupportedMime("IMAGE/PNG"))
        assertTrue(AttachmentValidator.isSupportedMime("application/pdf"))
        assertFalse(AttachmentValidator.isSupportedMime("text/plain"))
        assertFalse(AttachmentValidator.isSupportedMime("application/octet-stream"))
        assertFalse(AttachmentValidator.isSupportedMime("application/x-msdownload"))
    }

    @Test
    fun validateRejectsUnsupportedEmptyAndOversize() {
        assertEquals(AttachmentError.UNSUPPORTED_TYPE, AttachmentValidator.validate("text/csv", 10))
        assertEquals(AttachmentError.EMPTY, AttachmentValidator.validate("image/png", 0))
        assertEquals(AttachmentError.TOO_LARGE, AttachmentValidator.validate("image/png", AttachmentValidator.MAX_BYTES + 1))
        assertNull(AttachmentValidator.validate("image/png", 1_000))
        assertNull(AttachmentValidator.validate("application/pdf", null))
    }

    @Test
    fun resolveTypeMapsMimeAndOrigin() {
        assertEquals(AttachmentType.RECEIPT_IMAGE, AttachmentValidator.resolveType(AttachmentType.RECEIPT_IMAGE, "image/jpeg"))
        assertEquals(AttachmentType.SUPPORT_IMAGE, AttachmentValidator.resolveType(AttachmentType.SUPPORT_IMAGE, "image/png"))
        assertEquals(AttachmentType.DOCUMENT_PDF, AttachmentValidator.resolveType(AttachmentType.RECEIPT_IMAGE, "application/pdf"))
        // pedir un tipo no compatible con imagen cae a imagen de soporte
        assertEquals(AttachmentType.SUPPORT_IMAGE, AttachmentValidator.resolveType(AttachmentType.DOCUMENT_PDF, "image/png"))
        assertNull(AttachmentValidator.resolveType(AttachmentType.SUPPORT_IMAGE, "text/plain"))
    }

    @Test
    fun previewAvailableOnlyForImagesAndPdf() {
        assertTrue(AttachmentValidator.previewAvailable("image/png"))
        assertTrue(AttachmentValidator.previewAvailable("application/pdf"))
        assertFalse(AttachmentValidator.previewAvailable("text/plain"))
    }

    @Test
    fun sanitizeDisplayNameStripsPathsAndControlChars() {
        assertEquals("passwd", AttachmentValidator.sanitizeDisplayName("../../etc/passwd"))
        assertEquals("recibo.pdf", AttachmentValidator.sanitizeDisplayName("C:\\docs\\recibo.pdf"))
        assertEquals("ab", AttachmentValidator.sanitizeDisplayName("  a\tb  "))
        assertEquals("adjunto", AttachmentValidator.sanitizeDisplayName("   "))
        assertEquals("recibo.jpg", AttachmentValidator.sanitizeDisplayName("recibo.jpg"))
    }

    @Test
    fun sanitizeDisplayNameLimitsLength() {
        val long = "x".repeat(500)
        assertEquals(120, AttachmentValidator.sanitizeDisplayName(long).length)
    }

    @Test
    fun sumSizesIsOverflowSafe() {
        assertEquals(6L, AttachmentValidator.sumSizes(listOf(1L, 2L, 3L)))
        assertEquals(0L, AttachmentValidator.sumSizes(emptyList()))
    }

    @Test(expected = ArithmeticException::class)
    fun safeAddSizeThrowsOnOverflow() {
        AttachmentValidator.safeAddSize(Long.MAX_VALUE, 1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun safeAddSizeRejectsNegative() {
        AttachmentValidator.safeAddSize(-1L, 2L)
    }
}
