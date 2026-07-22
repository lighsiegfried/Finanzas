package com.kratt.finanzas.data.attachment

import android.content.Context
import com.kratt.finanzas.common.AttachmentChecksum
import com.kratt.finanzas.data.security.DatabaseKeyManager
import com.kratt.finanzas.data.security.DatabasePassphraseProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom

// error interno del almacenamiento de adjuntos; los mensajes no llevan datos sensibles
sealed class AttachmentStoreException(message: String) : Exception(message) {
    class TooLarge : AttachmentStoreException("too large")
    class Empty : AttachmentStoreException("empty")
    class NoSpace : AttachmentStoreException("no space")
    class Io : AttachmentStoreException("io")
    class Corrupt : AttachmentStoreException("corrupt")
    class Missing : AttachmentStoreException("missing")
}

// resultado de guardar un archivo cifrado
data class StoredFile(
    val storedFileName: String,
    val sizeBytes: Long,
    val checksum: String,
)

// guarda y recupera los archivos de adjuntos cifrados con aes-256-gcm en almacenamiento privado
// la clave maestra de 32 bytes se envuelve con una clave de android keystore, igual que la base
class AttachmentFileStore(
    context: Context,
    keyAlias: String = ATTACHMENT_KEY_ALIAS,
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    private val appContext = context.applicationContext
    private val dir: File = File(appContext.filesDir, "attachments").apply { mkdirs() }
    private val securityDir: File = File(appContext.noBackupFilesDir, "security").apply { mkdirs() }
    private val envelopeFile = File(securityDir, "attachments_key_v1.bin")
    private val keyManager = DatabaseKeyManager(keyAlias)
    // reutiliza el patron de sobre de la base para crear y recuperar la clave maestra
    private val keyProvider = DatabasePassphraseProvider(envelopeFile, keyManager, secureRandom)

    private fun masterKey(): ByteArray = keyProvider.getOrCreate()

    // lee el stream con un limite, cifra y escribe el archivo; valida vacio y tamano
    fun store(input: InputStream, maxBytes: Long): StoredFile {
        val bytes = readBounded(input, maxBytes)
        return storeBytes(bytes)
    }

    // cifra bytes ya en memoria y los escribe; se usa al importar y al restaurar un respaldo
    fun storeBytes(plaintext: ByteArray): StoredFile {
        if (plaintext.isEmpty()) throw AttachmentStoreException.Empty()
        val checksum = AttachmentChecksum.sha256Hex(plaintext)
        val encoded = encrypt(plaintext)
        // revisa que haya espacio antes de escribir
        if (dir.usableSpace < encoded.size.toLong() + SPACE_MARGIN) throw AttachmentStoreException.NoSpace()
        val name = randomName()
        val target = File(dir, name)
        val tmp = File(dir, "$name.tmp")
        try {
            tmp.writeBytes(encoded)
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
        } catch (e: IOException) {
            tmp.delete()
            throw AttachmentStoreException.NoSpace()
        }
        return StoredFile(name, plaintext.size.toLong(), checksum)
    }

    fun readDecrypted(storedFileName: String): ByteArray {
        val file = File(dir, storedFileName)
        if (!file.exists()) throw AttachmentStoreException.Missing()
        val raw = try {
            file.readBytes()
        } catch (e: IOException) {
            throw AttachmentStoreException.Io()
        }
        return decrypt(raw)
    }

    // igual que readDecrypted pero devuelve null en vez de lanzar; util para respaldos y barridos
    fun readDecryptedOrNull(storedFileName: String): ByteArray? =
        try {
            readDecrypted(storedFileName)
        } catch (e: Exception) {
            null
        }

    // cifra y escribe con un nombre exacto; se usa al restaurar para calzar con los metadatos del respaldo
    fun writeForName(storedFileName: String, plaintext: ByteArray) {
        val encoded = encrypt(plaintext)
        if (dir.usableSpace < encoded.size.toLong() + SPACE_MARGIN) throw AttachmentStoreException.NoSpace()
        val target = File(dir, storedFileName)
        val tmp = File(dir, "$storedFileName.tmp")
        tmp.writeBytes(encoded)
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    // descifra a un archivo temporal de cache para vistas previas; el llamador lo borra al cerrar
    fun decryptToCache(storedFileName: String, suffix: String): File {
        val bytes = readDecrypted(storedFileName)
        val cacheDir = File(appContext.cacheDir, "attachments").apply { mkdirs() }
        val out = File.createTempFile("att_", suffix, cacheDir)
        out.writeBytes(bytes)
        return out
    }

    fun delete(storedFileName: String): Boolean {
        val f = File(dir, storedFileName)
        return if (f.exists()) f.delete() else true
    }

    fun exists(storedFileName: String): Boolean = File(dir, storedFileName).exists()

    // nombres de archivo presentes en la carpeta, sin los temporales
    fun storedFileNames(): Set<String> =
        dir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".tmp") }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()

    private fun encrypt(plaintext: ByteArray): ByteArray {
        val key = masterKey()
        try {
            return AttachmentCipher.encode(key, plaintext, secureRandom)
        } finally {
            key.fill(0)
        }
    }

    private fun decrypt(raw: ByteArray): ByteArray {
        val key = masterKey()
        return try {
            AttachmentCipher.decode(key, raw)
        } catch (e: AttachmentCipherException) {
            throw AttachmentStoreException.Corrupt()
        } finally {
            key.fill(0)
        }
    }

    private fun randomName(): String {
        val b = ByteArray(16).also { secureRandom.nextBytes(it) }
        return b.joinToString("") { "%02x".format(it) } + ".enc"
    }

    // lee el stream hasta el limite; si se pasa lanza too large, si viene vacio lanza empty
    private fun readBounded(input: InputStream, maxBytes: Long): ByteArray {
        val buffer = ByteArray(8 * 1024)
        val out = ByteArrayOutputStream()
        var total = 0L
        while (true) {
            val r = try {
                input.read(buffer)
            } catch (e: IOException) {
                throw AttachmentStoreException.Io()
            }
            if (r < 0) break
            total += r
            if (total > maxBytes) throw AttachmentStoreException.TooLarge()
            out.write(buffer, 0, r)
        }
        if (total == 0L) throw AttachmentStoreException.Empty()
        return out.toByteArray()
    }

    companion object {
        const val ATTACHMENT_KEY_ALIAS = "finanzas_attachments_wrapping_key_v1"
        private const val SPACE_MARGIN = 1L * 1024 * 1024
    }
}
