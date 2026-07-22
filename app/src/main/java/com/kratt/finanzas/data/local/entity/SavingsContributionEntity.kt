package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kratt.finanzas.domain.model.ContributionType

// aporte a una meta; si es transferencia real queda ligado al movimiento generado
@Entity(
    tableName = "savings_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["savingsGoalId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedTransactionId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("savingsGoalId"),
        Index("sourceAccountId"),
        Index("linkedTransactionId"),
        Index("contributionDate"),
    ],
)
data class SavingsContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val savingsGoalId: Long,
    val amountCents: Long,
    // fecha guardada como dia epoch
    val contributionDate: Long,
    val sourceAccountId: Long? = null,
    val linkedTransactionId: Long? = null,
    val contributionType: ContributionType,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
