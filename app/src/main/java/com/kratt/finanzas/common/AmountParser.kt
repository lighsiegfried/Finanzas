package com.kratt.finanzas.common

import java.util.Locale

// convierte el texto del monto a centavos usando solo enteros, sin float ni double
object AmountParser {

    // hasta diez digitos enteros y maximo dos decimales, con punto o coma
    private val completePattern = Regex("""^\d{1,10}([.,]\d{0,2})?$""")
    private val partialPattern = Regex("""^\d{0,10}([.,]\d{0,2})?$""")

    // valida lo que el usuario va escribiendo para no dejar pasar letras
    fun isPartialInput(text: String): Boolean = partialPattern.matches(text)

    // devuelve el monto en centavos o null si el texto no es valido o es cero
    fun parseToCents(text: String): Long? {
        val clean = text.trim()
        if (!completePattern.matches(clean)) return null
        val parts = clean.replace(',', '.').split('.')
        val whole = parts[0].toLong()
        val fraction = if (parts.size > 1) parts[1].padEnd(2, '0') else "00"
        val cents = whole * 100 + fraction.toLong()
        return if (cents > 0) cents else null
    }

    // igual que parseToCents pero permite cero, sirve para el saldo inicial
    fun parseAmountAllowingZero(text: String): Long? {
        val clean = text.trim()
        if (clean.isEmpty()) return 0L
        if (!completePattern.matches(clean)) return null
        val parts = clean.replace(',', '.').split('.')
        val whole = parts[0].toLong()
        val fraction = if (parts.size > 1) parts[1].padEnd(2, '0') else "00"
        return whole * 100 + fraction.toLong()
    }

    // formatea centavos como numero editable sin simbolo ni separador de miles
    fun formatCents(cents: Long): String =
        String.format(Locale.US, "%d.%02d", cents / 100, (cents % 100).toInt())
}
