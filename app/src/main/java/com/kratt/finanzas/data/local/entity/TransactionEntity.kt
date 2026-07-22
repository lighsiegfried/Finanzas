package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kratt.finanzas.domain.model.TransactionType

// la fecha del movimiento se guarda como epoch day para evitar problemas de zona horaria
// en una transferencia accountId es el origen y destinationAccountId es el destino, sin categoria
@Entity(
    tableName = "transactions",
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
        Index("transactionDate"),
        // indices compuestos para acelerar los reportes por periodo y alcance
        Index("categoryId", "transactionDate"),
        Index("accountId", "transactionDate"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val categoryId: Long? = null,
    val type: TransactionType,
    val amountCents: Long,
    val description: String?,
    val transactionDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    // marca estable del origen del movimiento, por ejemplo "installment:12" o "recurring:5"
    val originKey: String? = null,
)
