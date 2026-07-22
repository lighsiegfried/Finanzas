package com.kratt.finanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kratt.finanzas.data.local.TransactionWithNames
import com.kratt.finanzas.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): TransactionEntity?

    // todos los movimientos, sirve para calcular los saldos de las cuentas
    @Query("SELECT * FROM transactions")
    fun observeAll(): Flow<List<TransactionEntity>>

    // proyeccion comun con nombres, categoria y cuenta destino resueltas
    // se usa left join porque las transferencias no tienen categoria
    @Query(
        "SELECT t.id AS id, t.type AS type, t.amountCents AS amountCents, " +
            "t.description AS description, t.transactionDate AS transactionDate, " +
            "t.accountId AS accountId, t.categoryId AS categoryId, " +
            "t.destinationAccountId AS destinationAccountId, " +
            "a.name AS accountName, c.name AS categoryName, d.name AS destinationAccountName " +
            "FROM transactions t " +
            "INNER JOIN accounts a ON a.id = t.accountId " +
            "LEFT JOIN categories c ON c.id = t.categoryId " +
            "LEFT JOIN accounts d ON d.id = t.destinationAccountId " +
            "ORDER BY t.transactionDate DESC, t.id DESC",
    )
    fun observeAllWithNames(): Flow<List<TransactionWithNames>>

    // igual que la anterior pero limitada a los mas recientes
    @Query(
        "SELECT t.id AS id, t.type AS type, t.amountCents AS amountCents, " +
            "t.description AS description, t.transactionDate AS transactionDate, " +
            "t.accountId AS accountId, t.categoryId AS categoryId, " +
            "t.destinationAccountId AS destinationAccountId, " +
            "a.name AS accountName, c.name AS categoryName, d.name AS destinationAccountName " +
            "FROM transactions t " +
            "INNER JOIN accounts a ON a.id = t.accountId " +
            "LEFT JOIN categories c ON c.id = t.categoryId " +
            "LEFT JOIN accounts d ON d.id = t.destinationAccountId " +
            "ORDER BY t.transactionDate DESC, t.id DESC " +
            "LIMIT :limit",
    )
    fun observeRecentWithNames(limit: Int): Flow<List<TransactionWithNames>>

    // movimientos del mes con nombres, para el resumen y los filtros
    @Query(
        "SELECT t.id AS id, t.type AS type, t.amountCents AS amountCents, " +
            "t.description AS description, t.transactionDate AS transactionDate, " +
            "t.accountId AS accountId, t.categoryId AS categoryId, " +
            "t.destinationAccountId AS destinationAccountId, " +
            "a.name AS accountName, c.name AS categoryName, d.name AS destinationAccountName " +
            "FROM transactions t " +
            "INNER JOIN accounts a ON a.id = t.accountId " +
            "LEFT JOIN categories c ON c.id = t.categoryId " +
            "LEFT JOIN accounts d ON d.id = t.destinationAccountId " +
            "WHERE t.transactionDate BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY t.transactionDate DESC, t.id DESC",
    )
    fun observeBetweenWithNames(startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionWithNames>>

    // trae los movimientos de un rango de dias, sirve para el mes elegido
    @Query(
        "SELECT * FROM transactions " +
            "WHERE transactionDate BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY transactionDate DESC, id DESC",
    )
    fun observeBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionEntity>>

    // totales por tipo de una cuenta como origen, sirve para el saldo
    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM transactions " +
            "WHERE accountId = :accountId AND type = :type",
    )
    suspend fun sumByAccountAndType(accountId: Long, type: String): Long

    // suma de transferencias que entran a una cuenta
    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM transactions " +
            "WHERE destinationAccountId = :accountId AND type = 'TRANSFER'",
    )
    suspend fun sumTransfersInto(accountId: Long): Long

    // cuenta cuantos movimientos usan una categoria, para no cambiar su tipo ni borrarla
    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Long

    // cuenta cuantos movimientos usan una cuenta como origen o destino
    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId OR destinationAccountId = :accountId")
    suspend fun countByAccount(accountId: Long): Long

    // ---- agregaciones de reportes, se usa sql para no cargar todo en memoria ----

    // suma de un tipo en un rango, sirve para ingresos, gastos y totales del periodo
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = :type AND transactionDate BETWEEN :start AND :end")
    suspend fun sumByTypeBetween(type: String, start: Long, end: Long): Long

    // gasto real del presupuesto general del periodo
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = 'EXPENSE' AND transactionDate BETWEEN :start AND :end")
    suspend fun sumExpensesBetween(start: Long, end: Long): Long

    // gasto real de una categoria en el periodo
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = 'EXPENSE' AND categoryId = :categoryId AND transactionDate BETWEEN :start AND :end")
    suspend fun sumExpensesForCategoryBetween(categoryId: Long, start: Long, end: Long): Long

    @Query(
        "SELECT c.id AS categoryId, c.name AS name, COALESCE(SUM(t.amountCents), 0) AS totalCents, COUNT(t.id) AS movementCount " +
            "FROM transactions t INNER JOIN categories c ON c.id = t.categoryId " +
            "WHERE t.type = 'EXPENSE' AND t.transactionDate BETWEEN :start AND :end " +
            "GROUP BY t.categoryId ORDER BY totalCents DESC",
    )
    suspend fun expensesByCategoryBetween(start: Long, end: Long): List<com.kratt.finanzas.data.local.CategoryTotalRow>

    @Query(
        "SELECT a.id AS accountId, a.name AS name, COALESCE(SUM(t.amountCents), 0) AS totalCents, COUNT(t.id) AS movementCount " +
            "FROM transactions t INNER JOIN accounts a ON a.id = t.accountId " +
            "WHERE t.type = 'EXPENSE' AND t.transactionDate BETWEEN :start AND :end " +
            "GROUP BY t.accountId ORDER BY totalCents DESC",
    )
    suspend fun expensesByAccountBetween(start: Long, end: Long): List<com.kratt.finanzas.data.local.AccountTotalRow>

    // totales de una cuenta como origen en un rango
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE accountId = :accountId AND type = :type AND transactionDate BETWEEN :start AND :end")
    suspend fun sumAccountTypeBetween(accountId: Long, type: String, start: Long, end: Long): Long

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE destinationAccountId = :accountId AND type = 'TRANSFER' AND transactionDate BETWEEN :start AND :end")
    suspend fun sumTransfersIntoBetween(accountId: Long, start: Long, end: Long): Long

    // totales de una cuenta antes del rango, para el saldo de apertura
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE accountId = :accountId AND type = :type AND transactionDate < :start")
    suspend fun sumAccountTypeBefore(accountId: Long, type: String, start: Long): Long

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE destinationAccountId = :accountId AND type = 'TRANSFER' AND transactionDate < :start")
    suspend fun sumTransfersIntoBefore(accountId: Long, start: Long): Long
}
