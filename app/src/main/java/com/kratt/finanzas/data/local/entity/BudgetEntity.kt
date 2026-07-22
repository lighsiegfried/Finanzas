package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// presupuesto mensual, categoryId nulo significa presupuesto general del mes
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["year", "month", "categoryId"], unique = true),
        Index("year", "month"),
        Index("categoryId"),
    ],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val year: Int,
    val month: Int,
    val categoryId: Long? = null,
    val limitAmountCents: Long,
    val warningPercentage: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
