package com.kratt.finanzas.common

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// da formato a los montos en quetzales a partir de centavos en long
object CurrencyFormatter {

    private const val CURRENCY_SYMBOL = "Q"

    // simbolos fijos para que el resultado no dependa del idioma del telefono
    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    private val decimalFormat = DecimalFormat("#,##0.00", symbols)

    // convierte centavos a texto, por ejemplo 123456 queda como Q1,234.56
    fun format(amountInCents: Long): String {
        val amount = BigDecimal.valueOf(amountInCents, 2)
        val sign = if (amount.signum() < 0) "-" else ""
        return sign + CURRENCY_SYMBOL + decimalFormat.format(amount.abs())
    }
}
