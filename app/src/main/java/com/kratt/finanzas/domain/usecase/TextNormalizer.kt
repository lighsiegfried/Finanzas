package com.kratt.finanzas.domain.usecase

import java.text.Normalizer

// normaliza texto para buscar sin importar mayusculas ni acentos
object TextNormalizer {

    private val marks = Regex("\\p{Mn}+")

    fun normalize(input: String): String {
        val lowered = input.lowercase()
        val decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD)
        return marks.replace(decomposed, "").trim()
    }
}
