package com.kratt.finanzas.data.backup

import java.nio.ByteBuffer

// encabezado publico del respaldo, se autentica como aad del payload cifrado
// disposicion fija en v1: salt de 16 y iv de 12
class BackupHeader(
    val kdf: KdfParams,
    val iv: ByteArray,
    val payloadLength: Long,
    val formatVersion: Int = FORMAT_VERSION,
    val backupType: Int = TYPE_FULL_DB,
) {
    fun encode(): ByteArray {
        val buffer = ByteBuffer.allocate(SIZE)
        buffer.put(MAGIC)
        buffer.put(formatVersion.toByte())
        buffer.put(backupType.toByte())
        buffer.put(KDF_ARGON2ID.toByte())
        buffer.put(Argon2idKdf.VERSION.toByte())
        buffer.putInt(kdf.memoryKiB)
        buffer.putInt(kdf.iterations)
        buffer.put(kdf.parallelism.toByte())
        buffer.put(kdf.outputLength.toByte())
        buffer.put(kdf.salt.size.toByte())
        buffer.put(kdf.salt)
        buffer.put(ENC_AES_256_GCM.toByte())
        buffer.put(iv.size.toByte())
        buffer.put(iv)
        buffer.putLong(payloadLength)
        return buffer.array()
    }

    companion object {
        val MAGIC = byteArrayOf(0x46, 0x5A, 0x42, 0x4B)
        const val FORMAT_VERSION = 1
        const val TYPE_FULL_DB = 1
        const val KDF_ARGON2ID = 1
        const val ENC_AES_256_GCM = 1
        const val SALT_LENGTH = 16
        const val IV_LENGTH = 12
        const val SIZE = 57
        // el payload puede incluir la base y, opcionalmente, los adjuntos cifrados del respaldo
        const val MAX_PAYLOAD = 512L * 1024 * 1024
        private const val MIN_MEMORY_KIB = 8
        private const val MAX_MEMORY_KIB = 1 shl 21

        // decodifica y valida cada campo antes de confiar en el, sin autenticar todavia
        fun decode(bytes: ByteArray): BackupHeader {
            if (bytes.size != SIZE) throw BackupFormatException.InvalidLength("header size")
            val buffer = ByteBuffer.wrap(bytes)
            val magic = ByteArray(4).also { buffer.get(it) }
            if (!magic.contentEquals(MAGIC)) throw BackupFormatException.BadMagic()
            val format = buffer.get().toInt() and 0xFF
            if (format != FORMAT_VERSION) throw BackupFormatException.UnsupportedVersion(format)
            val type = buffer.get().toInt() and 0xFF
            if (type != TYPE_FULL_DB) throw BackupFormatException.InvalidLength("type")
            val kdfId = buffer.get().toInt() and 0xFF
            if (kdfId != KDF_ARGON2ID) throw BackupFormatException.UnsupportedKdf(kdfId)
            val argonVersion = buffer.get().toInt() and 0xFF
            if (argonVersion != (Argon2idKdf.VERSION and 0xFF)) throw BackupFormatException.UnsupportedKdf(argonVersion)
            val memory = buffer.int
            val iterations = buffer.int
            val parallelism = buffer.get().toInt() and 0xFF
            val outputLength = buffer.get().toInt() and 0xFF
            val saltLength = buffer.get().toInt() and 0xFF
            if (memory < MIN_MEMORY_KIB || memory > MAX_MEMORY_KIB) throw BackupFormatException.InvalidLength("memory")
            if (iterations < 1 || iterations > 64) throw BackupFormatException.InvalidLength("iterations")
            if (parallelism < 1 || parallelism > 16) throw BackupFormatException.InvalidLength("parallelism")
            if (outputLength != KdfParams.OUTPUT_LENGTH) throw BackupFormatException.InvalidLength("outputLength")
            if (saltLength != SALT_LENGTH) throw BackupFormatException.InvalidLength("saltLength")
            val salt = ByteArray(saltLength).also { buffer.get(it) }
            val encId = buffer.get().toInt() and 0xFF
            if (encId != ENC_AES_256_GCM) throw BackupFormatException.UnsupportedEncryption(encId)
            val ivLength = buffer.get().toInt() and 0xFF
            if (ivLength != IV_LENGTH) throw BackupFormatException.InvalidLength("ivLength")
            val iv = ByteArray(ivLength).also { buffer.get(it) }
            val payloadLength = buffer.long
            if (payloadLength <= 0 || payloadLength > MAX_PAYLOAD) throw BackupFormatException.InvalidLength("payload")
            return BackupHeader(
                kdf = KdfParams(memory, iterations, parallelism, outputLength, salt),
                iv = iv,
                payloadLength = payloadLength,
                formatVersion = format,
                backupType = type,
            )
        }
    }
}
