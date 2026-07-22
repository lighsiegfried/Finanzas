package com.kratt.finanzas.common

import java.security.MessageDigest

// calcula la huella sha-256 en hexadecimal del contenido de un adjunto
object AttachmentChecksum {

    private val HEX = "0123456789abcdef".toCharArray()

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            val v = b.toInt() and 0xFF
            sb.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return sb.toString()
    }
}
