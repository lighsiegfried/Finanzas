package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kratt.finanzas.domain.model.InstallmentFrequency
import com.kratt.finanzas.domain.model.InstallmentStatus

// compra en cuotas, las fechas van como epoch day
@Entity(
    tableName = "installment_plans",
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
data class InstallmentPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val accountId: Long,
    val categoryId: Long,
    val totalAmountCents: Long,
    val installmentCount: Int,
    val installmentAmountCents: Long,
    val firstDueDate: Long,
    val frequency: InstallmentFrequency,
    val paidInstallments: Int,
    val status: InstallmentStatus,
    val description: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
