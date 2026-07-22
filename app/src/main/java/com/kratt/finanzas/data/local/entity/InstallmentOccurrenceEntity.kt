package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus

// una cuota concreta de una compra en cuotas
// paidTransactionId apunta al gasto generado; el fk restrict evita borrar ese gasto por fuera
@Entity(
    tableName = "installment_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = InstallmentPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["installmentPlanId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["paidTransactionId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["installmentPlanId", "sequenceNumber"], unique = true),
        Index("dueDate"),
        Index("status"),
        Index("paidTransactionId"),
    ],
)
data class InstallmentOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val installmentPlanId: Long,
    val sequenceNumber: Int,
    val dueDate: Long,
    val amountCents: Long,
    val status: InstallmentOccurrenceStatus,
    val paidTransactionId: Long? = null,
    val paidAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
