package com.kratt.finanzas.domain.repository

import com.kratt.finanzas.domain.model.Attachment
import com.kratt.finanzas.domain.model.AttachmentStorageSummary
import com.kratt.finanzas.domain.model.AttachmentType
import com.kratt.finanzas.domain.usecase.AddAttachmentResult
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.InputStream

// acceso a los adjuntos: metadatos en room y contenido cifrado en almacenamiento privado
interface AttachmentRepository {

    fun observeForTransaction(transactionId: Long): Flow<List<Attachment>>

    fun observeCountForTransaction(transactionId: Long): Flow<Int>

    fun observeStorageSummary(): Flow<AttachmentStorageSummary>

    suspend fun findById(id: Long): Attachment?

    suspend fun listForTransaction(transactionId: Long): List<Attachment>

    suspend fun listAll(): List<Attachment>

    // guarda el archivo cifrado y su metadato de forma consistente
    suspend fun add(
        transactionId: Long,
        displayName: String,
        mimeType: String,
        type: AttachmentType,
        input: InputStream,
    ): AddAttachmentResult

    // reinserta un adjunto desde bytes descifrados; se usa al restaurar un respaldo
    suspend fun restore(attachment: Attachment, plaintext: ByteArray): Long

    suspend fun delete(id: Long): Boolean

    // descifra el contenido en memoria para vista previa de imagenes
    suspend fun readBytes(attachment: Attachment): ByteArray?

    // descifra a un archivo temporal de cache para abrir un pdf; el llamador lo borra
    suspend fun decryptToCache(attachment: Attachment): File?

    // limpia archivos huerfanos que ya no tienen metadato
    suspend fun sweepOrphans()

    // contenido descifrado de todos los adjuntos para incluirlos en el respaldo (nombre -> bytes)
    suspend fun readAllForBackup(): List<Pair<String, ByteArray>>

    // escribe un archivo de adjunto restaurado desde bytes descifrados, con su nombre original
    suspend fun writeRestoredFile(storedFileName: String, plaintext: ByteArray)

    // elimina metadatos cuyos archivos ya no existen, por ejemplo tras restaurar sin adjuntos
    suspend fun pruneMissingFiles()
}
