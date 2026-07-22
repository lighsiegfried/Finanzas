package com.kratt.finanzas.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kratt.finanzas.data.local.entity.AccountEntity
import com.kratt.finanzas.data.local.entity.CategoryEntity
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.TransactionType

// inserta los datos iniciales la primera vez que se crea la base
class DefaultDataCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        DefaultDataSeeder.seed(db)
    }
}

object DefaultDataSeeder {

    // nombres visibles para el usuario, por eso van en espanol
    private val expenseCategories = listOf(
        "Alimentación" to "food",
        "Transporte" to "transport",
        "Hogar" to "home",
        "Educación" to "education",
        "Salud" to "health",
        "Entretenimiento" to "entertainment",
        "Servicios" to "services",
        "Compras" to "shopping",
        "Otros gastos" to "other",
    )

    private val incomeCategories = listOf(
        "Salario" to "salary",
        "Venta" to "sale",
        "Reembolso" to "refund",
        "Ingreso adicional" to "extra",
        "Otros ingresos" to "other",
    )

    // crea la cuenta efectivo y las categorias base, sin movimientos de ejemplo
    fun seed(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        db.execSQL(
            "INSERT INTO accounts (name, type, currencyCode, initialBalanceCents, isActive, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, 0, 1, ?, ?)",
            arrayOf<Any>("Efectivo", AccountType.CASH.name, "GTQ", now, now),
        )
        for ((name, iconKey) in expenseCategories) {
            insertCategory(db, name, TransactionType.EXPENSE, iconKey, now)
        }
        for ((name, iconKey) in incomeCategories) {
            insertCategory(db, name, TransactionType.INCOME, iconKey, now)
        }
    }

    // versiones en entidades para que las pruebas puedan reinsertar con los daos
    fun defaultAccount(now: Long): AccountEntity = AccountEntity(
        name = "Efectivo",
        type = AccountType.CASH,
        currencyCode = "GTQ",
        initialBalanceCents = 0L,
        isActive = true,
        createdAt = now,
        updatedAt = now,
    )

    fun defaultCategories(now: Long): List<CategoryEntity> =
        expenseCategories.map { (name, iconKey) ->
            categoryEntity(name, TransactionType.EXPENSE, iconKey, now)
        } + incomeCategories.map { (name, iconKey) ->
            categoryEntity(name, TransactionType.INCOME, iconKey, now)
        }

    private fun categoryEntity(
        name: String,
        type: TransactionType,
        iconKey: String,
        now: Long,
    ): CategoryEntity = CategoryEntity(
        name = name,
        transactionType = type,
        iconKey = iconKey,
        isDefault = true,
        isActive = true,
        createdAt = now,
        updatedAt = now,
    )

    private fun insertCategory(
        db: SupportSQLiteDatabase,
        name: String,
        type: TransactionType,
        iconKey: String,
        now: Long,
    ) {
        db.execSQL(
            "INSERT INTO categories (name, transactionType, iconKey, isDefault, isActive, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, 1, 1, ?, ?)",
            arrayOf<Any>(name, type.name, iconKey, now, now),
        )
    }
}
