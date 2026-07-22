package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kratt.finanzas.domain.model.SavingsGoalStatus

// meta de ahorro; el avance se calcula desde los aportes, no se guarda un total mutable
@Entity(
    tableName = "savings_goals",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("linkedAccountId"),
        Index("status"),
        Index("isArchived"),
    ],
)
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val targetAmountCents: Long,
    val linkedAccountId: Long? = null,
    // fechas guardadas como dia epoch
    val startDate: Long,
    val targetDate: Long? = null,
    val status: SavingsGoalStatus,
    val description: String? = null,
    val iconKey: String,
    val colorKey: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
