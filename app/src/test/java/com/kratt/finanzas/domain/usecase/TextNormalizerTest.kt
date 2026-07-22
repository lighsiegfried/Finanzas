package com.kratt.finanzas.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun lowercasesAndStripsAccents() {
        assertEquals("cafe", TextNormalizer.normalize("Café"))
        assertEquals("aeiou", TextNormalizer.normalize("ÁÉÍÓÚ"))
        assertEquals("nino", TextNormalizer.normalize("Niño"))
    }

    @Test
    fun trimsSurroundingSpaces() {
        assertEquals("salario", TextNormalizer.normalize("  Salario  "))
    }

    @Test
    fun accentInsensitiveContains() {
        val haystack = TextNormalizer.normalize("Alimentación")
        assertTrue(haystack.contains(TextNormalizer.normalize("alimentacion")))
    }
}
