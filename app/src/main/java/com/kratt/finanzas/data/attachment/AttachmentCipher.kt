package com.kratt.finanzas.data.attachment

import com.kratt.finanzas.data.backup.BackupCrypto
import java.security.SecureRandom

// se lanza cuando el archivo cifrado esta dañado o no se puede autenticar
class AttachmentCipherException(message: String) : Exception(message)

// formato del archivo de adjunto cifrado con aes-256-gcm
// disposicion: magic(4) | version(1) | ivLen(1) | iv(ivLen) | ciphertext+tag; el encabezado va como aad
object AttachmentCipher {

    // magic "FZA1" identifica el formato del archivo cifrado
    val MAGIC = byteArrayOf(0x46, 0x5A, 0x41, 0x31)
    const val VERSION = 1
    const val IV_LEN = 12
    private const val HEADER_FIXED = 6
    private const val TAG_BYTES = 16
    private const val HEADER_MIN = HEADER_FIXED + IV_LEN + TAG_BYTES

    // cifra con una clave de 32 bytes y un iv nuevo cada vez
    fun encode(key: ByteArray, plaintext: ByteArray, secureRandom: SecureRandom): ByteArray {
        val iv = ByteArray(IV_LEN).also { secureRandom.nextBytes(it) }
        val header = header(iv)
        val ciphertext = BackupCrypto.encrypt(key, iv, header, plaintext)
        return header + ciphertext
    }

    // valida el encabezado y descifra verificando el tag; lanza si algo no calza
    fun decode(key: ByteArray, raw: ByteArray): ByteArray {
        if (raw.size < HEADER_MIN) throw AttachmentCipherException("truncated")
        for (i in MAGIC.indices) if (raw[i] != MAGIC[i]) throw AttachmentCipherException("bad magic")
        val version = raw[4].toInt() and 0xFF
        if (version != VERSION) throw AttachmentCipherException("bad version")
        val ivLen = raw[5].toInt() and 0xFF
        if (ivLen != IV_LEN) throw AttachmentCipherException("bad iv")
        val cipherStart = HEADER_FIXED + ivLen
        if (raw.size < cipherStart + TAG_BYTES) throw AttachmentCipherException("truncated")
        val iv = raw.copyOfRange(HEADER_FIXED, cipherStart)
        val header = raw.copyOfRange(0, cipherStart)
        val ciphertext = raw.copyOfRange(cipherStart, raw.size)
        return try {
            BackupCrypto.decrypt(key, iv, header, ciphertext)
        } catch (e: Exception) {
            throw AttachmentCipherException("auth failed")
        }
    }

    private fun header(iv: ByteArray): ByteArray {
        val h = ByteArray(HEADER_FIXED + iv.size)
        System.arraycopy(MAGIC, 0, h, 0, MAGIC.size)
        h[4] = VERSION.toByte()
        h[5] = iv.size.toByte()
        System.arraycopy(iv, 0, h, HEADER_FIXED, iv.size)
        return h
    }
}
