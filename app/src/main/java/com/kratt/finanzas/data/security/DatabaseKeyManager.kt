package com.kratt.finanzas.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// se lanza cuando la clave de envoltura no permite recuperar la frase
class KeyUnwrapException(message: String) : Exception(message)

// maneja la clave de envoltura en android keystore y cifra la frase de la base
class DatabaseKeyManager(
    private val alias: String = DEFAULT_ALIAS,
) {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    // crea la clave aes en keystore si no existe, la clave no se puede exportar
    private fun getOrCreateWrappingKey(): SecretKey {
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    fun hasWrappingKey(): Boolean = keyStore.containsAlias(alias)

    // envuelve la frase con aes gcm, cada operacion usa un iv nuevo
    fun wrap(passphrase: ByteArray): KeyEnvelope {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())
        val ciphertext = cipher.doFinal(passphrase)
        return KeyEnvelope(cipher.iv.copyOf(), ciphertext)
    }

    // descifra la frase, cualquier fallo se traduce sin filtrar material sensible
    fun unwrap(envelope: KeyEnvelope): ByteArray {
        val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            ?: throw KeyUnwrapException("wrapping key missing")
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, entry.secretKey, GCMParameterSpec(GCM_TAG_BITS, envelope.iv))
            cipher.doFinal(envelope.ciphertext)
        } catch (e: Exception) {
            throw KeyUnwrapException("unwrap failed")
        }
    }

    companion object {
        const val DEFAULT_ALIAS = "finanzas_db_wrapping_key_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }
}
