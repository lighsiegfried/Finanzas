package com.kratt.finanzas.data.backup

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// arma el nombre sugerido del archivo de respaldo
object BackupFileName {
    const val EXTENSION = ".mfinanzas"
    const val MIME_TYPE = "application/octet-stream"
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")

    fun generate(dateTime: LocalDateTime): String =
        "mis-finanzas-${formatter.format(dateTime)}$EXTENSION"
}
