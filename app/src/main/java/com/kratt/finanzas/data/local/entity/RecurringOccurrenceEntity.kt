package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kratt.finanzas.domain.model.RecurringOccurrenceStatus

// una ocurrencia concreta de un movimiento recurrente
@Entity(
    tableName = "recurring_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = RecurringTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringTemplateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["generatedTransactionId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["recurringTemplateId", "scheduledDate"], unique = true),
        Index("scheduledDate"),
        Index("status"),
        Index("generatedTransactionId"),
    ],
)
data class RecurringOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recurringTemplateId: Long,
    val scheduledDate: Long,
    val amountCents: Long,
    val status: RecurringOccurrenceStatus,
    val generatedTransactionId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
