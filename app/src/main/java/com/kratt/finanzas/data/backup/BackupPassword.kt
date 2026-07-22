package com.kratt.finanzas.data.backup

// reglas de la contrasena del respaldo, sin recortes ni requisitos arbitrarios
object BackupPassword {
    const val MIN_LENGTH = 12
    const val MAX_CODE_POINTS = 128

    fun codePoints(password: String): Int = password.codePointCount(0, password.length)
    fun isEmpty(password: String): Boolean = password.isEmpty()
    fun isTooShort(password: String): Boolean = codePoints(password) < MIN_LENGTH
    fun isTooLong(password: String): Boolean = codePoints(password) > MAX_CODE_POINTS

    // compara la confirmacion exacta, sin recortar espacios
    fun matches(password: String, confirmation: String): Boolean = password == confirmation
}
