package com.kratt.finanzas.data.backup

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

// arma y parsea el respaldo portable: encabezado publico + payload cifrado autenticado
object PortableBackupCodec {

    private const val GCM_TAG_BYTES = 16

    // escribe el respaldo en el stream: encabezado como aad y payload cifrado
    fun write(
        output: OutputStream,
        password: ByteArray,
        manifest: BackupManifest,
        secureRandom: SecureRandom = SecureRandom(),
    ) {
        val salt = ByteArray(KdfParams.SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val kdf = KdfParams.defaultProfile(salt)
        val key = Argon2idKdf.derive(password, kdf)
        val plaintext = manifest.encode()
        try {
            val iv = ByteArray(BackupHeader.IV_LENGTH).also { secureRandom.nextBytes(it) }
            val payloadLength = (plaintext.size + GCM_TAG_BYTES).toLong()
            val headerBytes = BackupHeader(kdf, iv, payloadLength).encode()
            val ciphertext = BackupCrypto.encrypt(key, iv, headerBytes, plaintext)
            output.write(headerBytes)
            output.write(ciphertext)
            output.flush()
        } finally {
            key.fill(0)
            plaintext.fill(0)
        }
    }

    // lee el encabezado, deriva con la contrasena y descifra el payload autenticado
    fun readManifest(input: InputStream, password: ByteArray): BackupManifest {
        val headerBytes = readExactly(input, BackupHeader.SIZE)
        val header = BackupHeader.decode(headerBytes)
        val key = Argon2idKdf.derive(password, header.kdf)
        try {
            val ciphertext = readExactly(input, header.payloadLength.toInt())
            // no debe haber datos criticos extra despues del payload
            if (input.read() != -1) throw BackupFormatException.InvalidLength("trailing data")
            val plaintext = try {
                BackupCrypto.decrypt(key, header.iv, headerBytes, ciphertext)
            } catch (e: AEADBadTagException) {
                throw BackupFormatException.AuthenticationFailed()
            } catch (e: BackupFormatException) {
                throw e
            } catch (e: Exception) {
                throw BackupFormatException.AuthenticationFailed()
            }
            return BackupManifest.decode(plaintext)
        } finally {
            key.fill(0)
        }
    }

    // lee y valida solo el encabezado publico, sin autenticar el contenido
    fun readHeader(input: InputStream): BackupHeader =
        BackupHeader.decode(readExactly(input, BackupHeader.SIZE))

    private fun readExactly(input: InputStream, n: Int): ByteArray {
        if (n < 0) throw BackupFormatException.InvalidLength("negative")
        val buffer = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = input.read(buffer, read, n - read)
            if (r < 0) throw BackupFormatException.Truncated()
            read += r
        }
        return buffer
    }
}
