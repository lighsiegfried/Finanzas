package com.kratt.finanzas.data.backup

// parametros del derivado de clave argon2id, viajan dentro de cada respaldo
data class KdfParams(
    val memoryKiB: Int,
    val iterations: Int,
    val parallelism: Int,
    val outputLength: Int,
    val salt: ByteArray,
) {
    companion object {
        const val OUTPUT_LENGTH = 32
        const val SALT_LENGTH = 16

        // perfil movil por defecto, se mide y ajusta en el dispositivo
        fun defaultProfile(salt: ByteArray) = KdfParams(
            memoryKiB = 32 * 1024,
            iterations = 3,
            parallelism = 1,
            outputLength = OUTPUT_LENGTH,
            salt = salt,
        )
    }
}
