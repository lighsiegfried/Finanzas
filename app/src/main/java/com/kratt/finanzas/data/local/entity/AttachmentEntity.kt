package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kratt.finanzas.domain.model.AttachmentType

// metadatos del adjunto; el contenido binario no se guarda aqui sino en un archivo cifrado privado
// si se borra el movimiento la fila se elimina en cascada y el caso de uso limpia el archivo
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("transactionId")],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val transactionId: Long,
    val displayName: String,
    val mimeType: String,
    // nombre opaco del archivo cifrado dentro de la carpeta privada de adjuntos
    val storedFileName: String,
    val sizeBytes: Long,
    // huella sha-256 en hexadecimal del contenido original
    val checksum: String,
    val attachmentType: AttachmentType,
    val previewAvailable: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
