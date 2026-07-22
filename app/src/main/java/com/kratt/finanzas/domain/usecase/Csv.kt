package com.kratt.finanzas.domain.usecase

// arma filas csv de forma segura para hojas de calculo
object Csv {

    // marca de orden de bytes utf-8, con escape para no dejar el bom crudo en el archivo fuente
    const val BOM = "\uFEFF"

    private val formulaTriggers = charArrayOf('=', '+', '-', '@')

    // campo de texto libre: previene inyeccion de formulas y luego escapa
    fun textField(value: String): String = escape(prefixIfFormula(value))

    // campo ya seguro como numeros o fechas: solo escapa, sin prefijo de formula
    fun field(value: String): String = escape(value)

    fun row(escapedFields: List<String>): String = escapedFields.joinToString(",")

    private fun prefixIfFormula(value: String): String =
        if (value.isNotEmpty() && value[0] in formulaTriggers) "'$value" else value

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
