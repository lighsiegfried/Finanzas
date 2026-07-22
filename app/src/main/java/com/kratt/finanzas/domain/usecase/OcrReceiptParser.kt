package com.kratt.finanzas.domain.usecase

import java.time.LocalDate

// datos propuestos a partir de un comprobante; nunca se aplican solos, solo se sugieren
data class OcrSuggestions(
    val merchant: String? = null,
    val dateEpochDay: Long? = null,
    val totalCents: Long? = null,
    val invoiceNumber: String? = null,
    val categoryHint: String? = null,
) {
    val hasAny: Boolean
        get() = merchant != null || dateEpochDay != null || totalCents != null ||
            invoiceNumber != null || categoryHint != null
}

// extrae datos probables de un texto de ocr; logica pura y determinista para poder probarla
// nunca modifica un movimiento, solo devuelve sugerencias para que el usuario las revise
object OcrReceiptParser {

    private val AMOUNT = Regex("(?i)(?:Q\\s?)?\\d[\\d.,]*\\d")
    private val TOTAL_LINE = Regex("(?i)\\btotal\\b")
    private val SUBTOTAL_LINE = Regex("(?i)sub\\s*total")
    // los delimitadores de palabra evitan falsos positivos dentro de otras palabras (por ejemplo "eco-no-mia")
    private val INVOICE = Regex("(?i)\\b(?:factura|fac|folio|serie|no)\\b\\.?\\s*[:#-]?\\s*([A-Za-z0-9][A-Za-z0-9-]{2,20})")
    private val ISO_DATE = Regex("\\b(\\d{4})-(\\d{1,2})-(\\d{1,2})\\b")
    private val DMY_LONG = Regex("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})\\b")
    private val DMY_SHORT = Regex("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2})\\b")

    fun parse(rawText: String): OcrSuggestions {
        val lines = rawText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        return OcrSuggestions(
            merchant = extractMerchant(lines),
            dateEpochDay = extractDate(rawText),
            totalCents = extractTotal(lines),
            invoiceNumber = extractInvoice(rawText),
            categoryHint = null,
        )
    }

    // el comercio suele ser la primera linea con letras que no es una fecha ni un monto
    private fun extractMerchant(lines: List<String>): String? =
        lines.firstOrNull { line ->
            val letters = line.count { it.isLetter() }
            val digits = line.count { it.isDigit() }
            letters >= 3 && line.length in 3..60 && letters >= digits && !looksLikeDate(line)
        }?.take(60)

    private fun looksLikeDate(line: String): Boolean =
        ISO_DATE.containsMatchIn(line) || DMY_LONG.containsMatchIn(line) || DMY_SHORT.containsMatchIn(line)

    // toma el monto de la linea del total; si no hay, usa el monto mas grande del ticket
    private fun extractTotal(lines: List<String>): Long? {
        val totalLine = lines.lastOrNull { TOTAL_LINE.containsMatchIn(it) && !SUBTOTAL_LINE.containsMatchIn(it) }
        if (totalLine != null) {
            val amounts = amountsInLine(totalLine)
            if (amounts.isNotEmpty()) return amounts.max()
        }
        val all = lines.flatMap { amountsInLine(it) }
        return all.maxOrNull()
    }

    private fun amountsInLine(line: String): List<Long> =
        AMOUNT.findAll(line).mapNotNull { parseAmountToCents(it.value) }.toList()

    private fun extractInvoice(text: String): String? =
        INVOICE.find(text)?.groupValues?.getOrNull(1)?.takeIf { it.length >= 3 }

    private fun extractDate(text: String): Long? {
        ISO_DATE.find(text)?.let {
            toEpochDay(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())?.let { d -> return d }
        }
        DMY_LONG.find(text)?.let {
            toEpochDay(it.groupValues[3].toInt(), it.groupValues[2].toInt(), it.groupValues[1].toInt())?.let { d -> return d }
        }
        DMY_SHORT.find(text)?.let {
            toEpochDay(2000 + it.groupValues[3].toInt(), it.groupValues[2].toInt(), it.groupValues[1].toInt())?.let { d -> return d }
        }
        return null
    }

    private fun toEpochDay(year: Int, month: Int, day: Int): Long? =
        try {
            LocalDate.of(year, month, day).toEpochDay()
        } catch (e: Exception) {
            null
        }

    // convierte un monto de texto a centavos con matematica entera; nunca acepta negativos
    fun parseAmountToCents(token: String): Long? {
        var s = token.trim().replace("Q", "", ignoreCase = true).replace(" ", "")
        if (s.isEmpty() || s.startsWith("-")) return null
        val hasDot = s.contains('.')
        val hasComma = s.contains(',')
        s = when {
            hasDot && hasComma -> {
                if (s.lastIndexOf('.') > s.lastIndexOf(',')) s.replace(",", "") else s.replace(".", "").replace(",", ".")
            }
            hasComma -> {
                val idx = s.lastIndexOf(',')
                if (s.length - idx - 1 == 2) s.replace(",", ".") else s.replace(",", "")
            }
            else -> s
        }
        if (!s.matches(Regex("\\d+(\\.\\d+)?"))) return null
        val parts = s.split(".")
        val intPart = parts[0]
        val frac2 = ((if (parts.size > 1) parts[1] else "") + "00").substring(0, 2)
        return try {
            Math.addExact(Math.multiplyExact(intPart.toLong(), 100L), frac2.toLong())
        } catch (e: ArithmeticException) {
            null
        }
    }
}
