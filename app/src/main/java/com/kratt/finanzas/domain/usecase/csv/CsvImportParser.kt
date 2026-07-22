package com.kratt.finanzas.domain.usecase.csv

// resultado de separar el csv: encabezado normalizado y filas crudas
data class ParsedCsv(val header: List<String>, val rows: List<RawCsvRow>)

// separa texto csv respetando comillas, comas y saltos de linea dentro de comillas
object CsvImportParser {

    // marca de orden de bytes escrita por codigo para no dejar un bom literal en el archivo
    private val bom = Char(0xFEFF).toString()

    fun parse(text: String): ParsedCsv {
        val clean = text.removePrefix(bom)
        val records = splitRecords(clean)
        if (records.isEmpty()) return ParsedCsv(emptyList(), emptyList())
        val header = records.first().map { it.trim().lowercase() }
        // ignora lineas totalmente vacias y numera las filas sin contar el encabezado
        var number = 0
        val rows = records.drop(1).mapNotNull { record ->
            if (record.size == 1 && record[0].isBlank()) {
                null
            } else {
                number++
                RawCsvRow(rowNumber = number, values = record)
            }
        }
        return ParsedCsv(header, rows)
    }

    // maquina de estados que arma registros y campos del csv
    private fun splitRecords(text: String): List<List<String>> {
        val records = ArrayList<List<String>>()
        var fields = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> { fields.add(field.toString()); field.setLength(0) }
                c == '\r' -> {
                    fields.add(field.toString()); field.setLength(0)
                    records.add(fields); fields = ArrayList()
                    if (i + 1 < text.length && text[i + 1] == '\n') i++
                }
                c == '\n' -> {
                    fields.add(field.toString()); field.setLength(0)
                    records.add(fields); fields = ArrayList()
                }
                else -> field.append(c)
            }
            i++
        }
        // agrega el ultimo registro si el archivo no termina en salto de linea
        if (field.isNotEmpty() || fields.isNotEmpty()) {
            fields.add(field.toString())
            records.add(fields)
        }
        return records
    }
}
