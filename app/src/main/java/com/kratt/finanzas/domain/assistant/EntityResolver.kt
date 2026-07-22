package com.kratt.finanzas.domain.assistant

import com.kratt.finanzas.domain.usecase.TextNormalizer

// resuelve cuentas o categorias mencionadas por nombre, sin importar mayusculas ni acentos
object EntityResolver {

    fun resolve(query: String, candidates: List<EntityCandidate>): EntityResolution {
        val haystack = tokenize(query)
        val matches = candidates.filter { it.name.isNotBlank() && haystack.contains(tokenize(it.name)) }
        return when {
            matches.isEmpty() -> EntityResolution.NotRequested
            matches.size == 1 -> EntityResolution.Unique(matches[0].id, matches[0].name)
            else -> {
                // si todos los nombres estan contenidos en el mas largo, es el mismo mas especifico
                val longest = matches.maxByOrNull { it.name.length }!!
                val longestTokens = tokenize(longest.name)
                val allNested = matches.all { longestTokens.contains(tokenize(it.name)) }
                if (allNested) {
                    EntityResolution.Unique(longest.id, longest.name)
                } else {
                    EntityResolution.Ambiguous(matches.map { EntityCandidate(it.id, it.name) })
                }
            }
        }
    }

    // deja solo letras y numeros separados por un espacio para comparar por palabras completas
    // el resultado va rodeado de espacios para exigir coincidencia de palabras completas
    private fun tokenize(input: String): String {
        val normalized = TextNormalizer.normalize(input)
        val cleaned = normalized.map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
        return " " + cleaned.split(" ").filter { it.isNotBlank() }.joinToString(" ") + " "
    }
}
