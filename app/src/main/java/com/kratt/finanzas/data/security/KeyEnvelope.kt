package com.kratt.finanzas.data.security

// errores del sobre de la clave, sus mensajes no llevan datos sensibles
sealed class KeyEnvelopeException(message: String) : Exception(message) {
    class BadMagic : KeyEnvelopeException("bad magic")
    class UnsupportedVersion(val version: Int) : KeyEnvelopeException("unsupported version")
    class Truncated : KeyEnvelopeException("truncated envelope")
    class BadLength : KeyEnvelopeException("bad length")
}

// sobre versionado que guarda la frase cifrada y su iv
// formato: magic(4) | version(1) | ivLen(1) | iv(ivLen) | ciphertext+tag(resto)
class KeyEnvelope(
    val iv: ByteArray,
    val ciphertext: ByteArray,
) {
    fun encode(): ByteArray {
        val out = ByteArray(HEADER + iv.size + ciphertext.size)
        System.arraycopy(MAGIC, 0, out, 0, MAGIC.size)
        out[4] = VERSION.toByte()
        out[5] = iv.size.toByte()
        System.arraycopy(iv, 0, out, HEADER, iv.size)
        System.arraycopy(ciphertext, 0, out, HEADER + iv.size, ciphertext.size)
        return out
    }

    companion object {
        // magic "FZK1" identifica el formato del sobre
        val MAGIC = byteArrayOf(0x46, 0x5A, 0x4B, 0x31)
        const val VERSION = 1
        private const val HEADER = 6
        private const val MIN_IV = 12
        private const val MIN_CIPHERTEXT = 16
        private const val MAX_SIZE = 512

        // decodifica y valida todas las longitudes antes de usar el sobre
        fun decode(bytes: ByteArray): KeyEnvelope {
            if (bytes.size < HEADER || bytes.size > MAX_SIZE) throw KeyEnvelopeException.BadLength()
            for (i in MAGIC.indices) if (bytes[i] != MAGIC[i]) throw KeyEnvelopeException.BadMagic()
            val version = bytes[4].toInt() and 0xFF
            if (version != VERSION) throw KeyEnvelopeException.UnsupportedVersion(version)
            val ivLen = bytes[5].toInt() and 0xFF
            if (ivLen < MIN_IV) throw KeyEnvelopeException.BadLength()
            val cipherStart = HEADER + ivLen
            if (bytes.size < cipherStart + MIN_CIPHERTEXT) throw KeyEnvelopeException.Truncated()
            val iv = bytes.copyOfRange(HEADER, cipherStart)
            val ciphertext = bytes.copyOfRange(cipherStart, bytes.size)
            return KeyEnvelope(iv, ciphertext)
        }
    }
}
