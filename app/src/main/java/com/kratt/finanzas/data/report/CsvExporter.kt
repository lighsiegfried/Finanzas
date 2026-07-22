package com.kratt.finanzas.data.report

import com.kratt.finanzas.domain.usecase.Csv
import java.io.OutputStream

// una celda de reporte: texto libre (se protege de formulas) o valor ya seguro
sealed interface CsvCell {
    data class Text(val value: String) : CsvCell
    data class Value(val value: String) : CsvCell
}

data class CsvTable(val header: List<String>, val rows: List<List<CsvCell>>)

class CsvExporter {

    // escribe el reporte en utf-8 con bom para que la hoja de calculo lea bien los acentos
    fun write(output: OutputStream, table: CsvTable) {
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(Csv.BOM)
            writer.write(Csv.row(table.header.map { Csv.textField(it) }))
            writer.write("\r\n")
            table.rows.forEach { row ->
                writer.write(Csv.row(row.map { cell -> escapeCell(cell) }))
                writer.write("\r\n")
            }
            writer.flush()
        }
    }

    private fun escapeCell(cell: CsvCell): String = when (cell) {
        is CsvCell.Text -> Csv.textField(cell.value)
        is CsvCell.Value -> Csv.field(cell.value)
    }
}
