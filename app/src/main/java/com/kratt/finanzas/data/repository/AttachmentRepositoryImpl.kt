package com.kratt.finanzas.data.repository

import com.kratt.finanzas.data.attachment.AttachmentFileStore
import com.kratt.finanzas.data.attachment.AttachmentStoreException
import com.kratt.finanzas.data.local.dao.AttachmentDao
import com.kratt.finanzas.data.local.entity.AttachmentEntity
import com.kratt.finanzas.domain.model.Attachment
import com.kratt.finanzas.domain.model.AttachmentStorageSummary
import com.kratt.finanzas.domain.model.AttachmentType
import com.kratt.finanzas.domain.repository.AttachmentRepository
import com.kratt.finanzas.domain.usecase.AddAttachmentResult
import com.kratt.finanzas.domain.usecase.AttachmentError
import com.kratt.finanzas.domain.usecase.AttachmentValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.InputStream

// mantiene consistente el metadato en room y el archivo cifrado en disco
class AttachmentRepositoryImpl(
    private val dao: AttachmentDao,
    private val store: AttachmentFileStore,
    private val nowMillis: () -> Long,
) : AttachmentRepository {

    override fun observeForTransaction(transactionId: Long): Flow<List<Attachment>> =
        dao.observeForTransaction(transactionId).map { list -> list.map { it.toDomain() } }

    override fun observeCountForTransaction(transactionId: Long): Flow<Int> =
        dao.observeCountForTransaction(transactionId)

    override fun observeStorageSummary(): Flow<AttachmentStorageSummary> =
        combine(dao.observeTotalCount(), dao.observeTotalBytes()) { count, bytes ->
            AttachmentStorageSummary(fileCount = count, totalBytes = bytes)
        }

    override suspend fun findById(id: Long): Attachment? = dao.findById(id)?.toDomain()

    override suspend fun listForTransaction(transactionId: Long): List<Attachment> =
        dao.listForTransaction(transactionId).map { it.toDomain() }

    override suspend fun listAll(): List<Attachment> = dao.listAll().map { it.toDomain() }

    override suspend fun add(
        transactionId: Long,
        displayName: String,
        mimeType: String,
        type: AttachmentType,
        input: InputStream,
    ): AddAttachmentResult {
        // primero se escribe el archivo cifrado; si falla no se toca la base
        val stored = try {
            store.store(input, AttachmentValidator.MAX_BYTES)
        } catch (e: AttachmentStoreException.TooLarge) {
            return AddAttachmentResult.Failure(AttachmentError.TOO_LARGE)
        } catch (e: AttachmentStoreException.Empty) {
            return AddAttachmentResult.Failure(AttachmentError.EMPTY)
        } catch (e: AttachmentStoreException.NoSpace) {
            return AddAttachmentResult.Failure(AttachmentError.NO_SPACE)
        } catch (e: AttachmentStoreException.Io) {
            return AddAttachmentResult.Failure(AttachmentError.UNREADABLE)
        } catch (e: Exception) {
            return AddAttachmentResult.Failure(AttachmentError.GENERIC)
        }
        // si el metadato falla, borra el archivo para no dejar huerfanos
        return try {
            val now = nowMillis()
            val id = dao.insert(
                AttachmentEntity(
                    transactionId = transactionId,
                    displayName = displayName,
                    mimeType = mimeType,
                    storedFileName = stored.storedFileName,
                    sizeBytes = stored.sizeBytes,
                    checksum = stored.checksum,
                    attachmentType = type,
                    previewAvailable = AttachmentValidator.previewAvailable(mimeType),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            AddAttachmentResult.Success(id)
        } catch (e: Exception) {
            store.delete(stored.storedFileName)
            AddAttachmentResult.Failure(AttachmentError.GENERIC)
        }
    }

    override suspend fun restore(attachment: Attachment, plaintext: ByteArray): Long {
        val stored = store.storeBytes(plaintext)
        val now = nowMillis()
        return dao.insert(
            AttachmentEntity(
                transactionId = attachment.transactionId,
                displayName = attachment.displayName,
                mimeType = attachment.mimeType,
                storedFileName = stored.storedFileName,
                sizeBytes = stored.sizeBytes,
                checksum = stored.checksum,
                attachmentType = attachment.attachmentType,
                previewAvailable = attachment.previewAvailable,
                createdAt = attachment.createdAt,
                updatedAt = now,
            ),
        )
    }

    override suspend fun delete(id: Long): Boolean {
        val existing = dao.findById(id) ?: return true
        // el metadato es la fuente de verdad: se borra primero y luego el archivo
        dao.deleteById(id)
        store.delete(existing.storedFileName)
        return true
    }

    override suspend fun readBytes(attachment: Attachment): ByteArray? =
        try {
            store.readDecrypted(attachment.storedFileName)
        } catch (e: Exception) {
            null
        }

    override suspend fun decryptToCache(attachment: Attachment): File? =
        try {
            val suffix = if (attachment.isPdf) ".pdf" else ".img"
            store.decryptToCache(attachment.storedFileName, suffix)
        } catch (e: Exception) {
            null
        }

    override suspend fun sweepOrphans() {
        val referenced = dao.listAll().map { it.storedFileName }.toSet()
        store.storedFileNames().forEach { name -> if (name !in referenced) store.delete(name) }
    }

    override suspend fun readAllForBackup(): List<Pair<String, ByteArray>> =
        dao.listAll().mapNotNull { row ->
            store.readDecryptedOrNull(row.storedFileName)?.let { row.storedFileName to it }
        }

    override suspend fun writeRestoredFile(storedFileName: String, plaintext: ByteArray) {
        store.writeForName(storedFileName, plaintext)
    }

    override suspend fun pruneMissingFiles() {
        dao.listAll().forEach { if (!store.exists(it.storedFileName)) dao.deleteById(it.id) }
    }
}

// mapea el metadato de room al modelo de dominio
private fun AttachmentEntity.toDomain(): Attachment = Attachment(
    id = id,
    transactionId = transactionId,
    displayName = displayName,
    mimeType = mimeType,
    storedFileName = storedFileName,
    sizeBytes = sizeBytes,
    checksum = checksum,
    attachmentType = attachmentType,
    previewAvailable = previewAvailable,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
