package com.kratt.finanzas.data.security

import java.io.File
import java.security.SecureRandom

// se lanza cuando existe la base cifrada pero no se puede recuperar su frase
class PassphraseUnavailableException(message: String) : Exception(message)

// entrega la frase de la base: la crea la primera vez o la recupera del sobre
class DatabasePassphraseProvider(
    private val envelopeFile: File,
    private val keyManager: DatabaseKeyManager,
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun envelopeExists(): Boolean = envelopeFile.exists()

    // genera 32 bytes aleatorios, los envuelve y guarda el sobre en disco
    fun createAndPersist(): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_BYTES).also { secureRandom.nextBytes(it) }
        val envelope = keyManager.wrap(passphrase)
        envelopeFile.parentFile?.mkdirs()
        // escribe en un temporal y luego renombra para no dejar sobres a medias
        val tmp = File(envelopeFile.parentFile, envelopeFile.name + ".tmp")
        tmp.writeBytes(envelope.encode())
        if (!tmp.renameTo(envelopeFile)) {
            tmp.copyTo(envelopeFile, overwrite = true)
            tmp.delete()
        }
        return passphrase
    }

    // recupera la frase del sobre existente, falla de forma segura si esta dañado
    fun load(): ByteArray {
        val raw = try {
            envelopeFile.readBytes()
        } catch (e: Exception) {
            throw PassphraseUnavailableException("envelope unreadable")
        }
        val envelope = KeyEnvelope.decode(raw)
        return try {
            keyManager.unwrap(envelope)
        } catch (e: KeyUnwrapException) {
            throw PassphraseUnavailableException("unwrap failed")
        }
    }

    fun getOrCreate(): ByteArray = if (envelopeExists()) load() else createAndPersist()

    companion object {
        const val PASSPHRASE_BYTES = 32
    }
}
