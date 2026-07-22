package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kratt.finanzas.domain.model.TransactionType

// plantilla reutilizable de movimiento; nunca crea un movimiento por si sola
@Entity(
    tableName = "transaction_templates",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("accountId"),
        Index("destinationAccountId"),
        Index("categoryId"),
        Index("isFavorite"),
        Index("isActive"),
    ],
)
data class TransactionTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val transactionType: TransactionType,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val categoryId: Long? = null,
    val defaultAmountCents: Long? = null,
    val description: String? = null,
    val isFavorite: Boolean,
    val isActive: Boolean,
    val lastUsedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
