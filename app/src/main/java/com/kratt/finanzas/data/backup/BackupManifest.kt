package com.kratt.finanzas.data.backup

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

// un adjunto que viaja dentro del respaldo: el nombre del archivo y su contenido descifrado
// el contenido queda protegido por el cifrado del propio respaldo (aes-256-gcm + argon2id)
class BackupAttachment(
    val storedFileName: String,
    val plaintext: ByteArray,
)

// contenido interno del payload ya descifrado y autenticado
class BackupManifest(
    val roomSchemaVersion: Int,
    val createdAtMillis: Long,
    val dbName: String,
    val passphrase: ByteArray,
    val encryptedDb: ByteArray,
    val attachments: List<BackupAttachment> = emptyList(),
    val manifestVersion: Int = MANIFEST_VERSION,
    val appDataVersion: Int = APP_DATA_VERSION,
) {
    fun encode(): ByteArray {
        val nameBytes = dbName.toByteArray(Charsets.UTF_8)
        val attNameBytes = if (manifestVersion >= 2) {
            attachments.map { it.storedFileName.toByteArray(Charsets.UTF_8) }
        } else {
            emptyList()
        }
        var size = 1 + 1 + 4 + 8 + 2 + nameBytes.size + 2 + passphrase.size + 8 + encryptedDb.size
        if (manifestVersion >= 2) {
            size += 4
            for (i in attachments.indices) size += 2 + attNameBytes[i].size + 8 + attachments[i].plaintext.size
        }
        val buffer = ByteBuffer.allocate(size)
        buffer.put(manifestVersion.toByte())
        buffer.put(appDataVersion.toByte())
        buffer.putInt(roomSchemaVersion)
        buffer.putLong(createdAtMillis)
        buffer.putShort(nameBytes.size.toShort())
        buffer.put(nameBytes)
        buffer.putShort(passphrase.size.toShort())
        buffer.put(passphrase)
        buffer.putLong(encryptedDb.size.toLong())
        buffer.put(encryptedDb)
        if (manifestVersion >= 2) {
            // seccion opcional de adjuntos: cantidad y luego cada archivo con su nombre y bytes
            buffer.putInt(attachments.size)
            for (i in attachments.indices) {
                buffer.putShort(attNameBytes[i].size.toShort())
                buffer.put(attNameBytes[i])
                buffer.putLong(attachments[i].plaintext.size.toLong())
                buffer.put(attachments[i].plaintext)
            }
        }
        return buffer.array()
    }

    companion object {
        const val MANIFEST_VERSION = 2
        // versiones de manifiesto que la restauracion acepta: 1 (previo a 5D) y 2 (con adjuntos)
        val SUPPORTED_MANIFEST_VERSIONS = setOf(1, 2)
        const val APP_DATA_VERSION = 1
        // esquema de room que escriben los respaldos nuevos
        const val CURRENT_ROOM_SCHEMA = 6
        // esquemas que la restauracion acepta y migra hacia adelante si hace falta
        val SUPPORTED_ROOM_SCHEMAS = setOf(1, 2, 3, 4, 5, 6)
        const val MAX_DB_SIZE = 64L * 1024 * 1024
        private const val MAX_NAME = 256
        private const val MAX_PASSPHRASE = 256
        // limites de la seccion de adjuntos para no confiar en un respaldo malicioso
        private const val MAX_ATTACHMENTS = 10_000
        private const val MAX_ATTACHMENT_BYTES = 20L * 1024 * 1024
        private const val MAX_TOTAL_ATTACHMENT_BYTES = 480L * 1024 * 1024

        // decodifica el manifiesto ya autenticado y rechaza cualquier campo imposible
        fun decode(bytes: ByteArray): BackupManifest {
            val buffer = ByteBuffer.wrap(bytes)
            fun need(n: Int) { if (n < 0 || buffer.remaining() < n) throw BackupFormatException.Truncated() }
            need(1 + 1 + 4 + 8 + 2)
            val manifestVersion = buffer.get().toInt() and 0xFF
            if (manifestVersion !in SUPPORTED_MANIFEST_VERSIONS) throw BackupFormatException.UnsupportedVersion(manifestVersion)
            val appDataVersion = buffer.get().toInt() and 0xFF
            if (appDataVersion != APP_DATA_VERSION) throw BackupFormatException.UnsupportedVersion(appDataVersion)
            val schema = buffer.int
            if (schema !in SUPPORTED_ROOM_SCHEMAS) throw BackupFormatException.UnsupportedSchema(schema)
            val created = buffer.long
            val nameLen = buffer.short.toInt() and 0xFFFF
            if (nameLen < 1 || nameLen > MAX_NAME) throw BackupFormatException.InvalidLength("dbName")
            need(nameLen)
            val nameBytes = ByteArray(nameLen).also { buffer.get(it) }
            val name = decodeStrictUtf8(nameBytes)
            need(2)
            val passLen = buffer.short.toInt() and 0xFFFF
            if (passLen < 1 || passLen > MAX_PASSPHRASE) throw BackupFormatException.InvalidLength("passphrase")
            need(passLen)
            val passphrase = ByteArray(passLen).also { buffer.get(it) }
            need(8)
            val dbLen = buffer.long
            if (dbLen < 1 || dbLen > MAX_DB_SIZE) throw BackupFormatException.InvalidLength("dbLength")
            need(dbLen.toInt())
            val db = ByteArray(dbLen.toInt()).also { buffer.get(it) }

            val attachments = if (manifestVersion >= 2) decodeAttachments(buffer, ::need) else emptyList()

            // no debe quedar nada extra despues del contenido esperado
            if (buffer.remaining() != 0) throw BackupFormatException.InvalidLength("trailing data")
            return BackupManifest(schema, created, name, passphrase, db, attachments, manifestVersion, appDataVersion)
        }

        // lee la seccion de adjuntos validando cada tamano antes de reservar memoria
        private fun decodeAttachments(buffer: ByteBuffer, need: (Int) -> Unit): List<BackupAttachment> {
            need(4)
            val count = buffer.int
            if (count < 0 || count > MAX_ATTACHMENTS) throw BackupFormatException.InvalidLength("attachmentCount")
            val list = ArrayList<BackupAttachment>(count)
            var total = 0L
            repeat(count) {
                need(2)
                val nameLen = buffer.short.toInt() and 0xFFFF
                if (nameLen < 1 || nameLen > MAX_NAME) throw BackupFormatException.InvalidLength("attachmentName")
                need(nameLen)
                val nameBytes = ByteArray(nameLen).also { buffer.get(it) }
                val storedName = decodeStrictUtf8(nameBytes)
                need(8)
                val payloadLen = buffer.long
                if (payloadLen < 0 || payloadLen > MAX_ATTACHMENT_BYTES) throw BackupFormatException.InvalidLength("attachmentPayload")
                total += payloadLen
                if (total > MAX_TOTAL_ATTACHMENT_BYTES) throw BackupFormatException.InvalidLength("attachmentTotal")
                need(payloadLen.toInt())
                val payload = ByteArray(payloadLen.toInt()).also { buffer.get(it) }
                list.add(BackupAttachment(storedName, payload))
            }
            return list
        }

        private fun decodeStrictUtf8(bytes: ByteArray): String {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            return try {
                decoder.decode(ByteBuffer.wrap(bytes)).toString()
            } catch (e: Exception) {
                throw BackupFormatException.InvalidLength("malformed utf8")
            }
        }
    }
}
