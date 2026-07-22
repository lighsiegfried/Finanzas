package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kratt.finanzas.domain.model.PostingMode
import com.kratt.finanzas.domain.model.RecurrenceType
import com.kratt.finanzas.domain.model.TransactionType

// plantilla de un movimiento recurrente, genera ocurrencias en una ventana acotada
@Entity(
    tableName = "recurring_templates",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("accountId"), Index("categoryId")],
)
data class RecurringTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val transactionType: TransactionType,
    val accountId: Long,
    val categoryId: Long,
    val amountCents: Long,
    val recurrenceType: RecurrenceType,
    val interval: Int,
    val startDate: Long,
    val endDate: Long? = null,
    val nextOccurrenceDate: Long,
    val postingMode: PostingMode,
    val isActive: Boolean,
    val description: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
