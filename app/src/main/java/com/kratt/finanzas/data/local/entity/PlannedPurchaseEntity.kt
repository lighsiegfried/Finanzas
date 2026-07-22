package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kratt.finanzas.domain.model.PurchasePriority
import com.kratt.finanzas.domain.model.PurchaseStatus

// compra planificada; no crea gasto hasta que se registra la compra
@Entity(
    tableName = "planned_purchases",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["savingsGoalId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchasedTransactionId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("categoryId"),
        Index("savingsGoalId"),
        Index("status"),
        Index("priority"),
        Index("purchasedTransactionId"),
    ],
)
data class PlannedPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val estimatedCostCents: Long,
    val categoryId: Long? = null,
    val savingsGoalId: Long? = null,
    // fecha guardada como dia epoch
    val targetDate: Long? = null,
    val priority: PurchasePriority,
    val status: PurchaseStatus,
    val description: String? = null,
    val vendor: String? = null,
    val purchasedTransactionId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
