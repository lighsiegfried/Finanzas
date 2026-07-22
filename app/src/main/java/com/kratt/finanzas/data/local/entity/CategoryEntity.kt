package com.kratt.finanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kratt.finanzas.domain.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val transactionType: TransactionType,
    val iconKey: String,
    val colorKey: String? = null,
    val isDefault: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
