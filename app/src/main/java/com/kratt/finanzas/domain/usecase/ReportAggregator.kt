package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.IncomeExpense
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType

// agrupa movimientos para los reportes; las transferencias nunca cuentan como gasto ni ingreso
object ReportAggregator {

    fun incomeExpense(transactions: List<Transaction>): IncomeExpense {
        var income = 0L
        var expense = 0L
        // sumas seguras, detectan desbordes en vez de girar en silencio
        for (t in transactions) {
            when (t.type) {
                TransactionType.INCOME -> income = MoneyMath.add(income, t.amountCents)
                TransactionType.EXPENSE -> expense = MoneyMath.add(expense, t.amountCents)
                TransactionType.TRANSFER -> Unit
            }
        }
        return IncomeExpense(income, expense)
    }

    // suma de gastos por categoria, ignora ingresos y transferencias
    fun expensesByCategory(transactions: List<Transaction>): Map<Long, Long> =
        transactions
            .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { entry -> MoneyMath.sum(entry.value.map { it.amountCents }) }

    // porcentaje entero de una parte sobre el total, protege la division
    fun percentageOfTotal(partCents: Long, totalCents: Long): Int =
        if (totalCents > 0) ((partCents * 100) / totalCents).toInt() else 0
}

// gasto real de un presupuesto: solo movimientos de gasto del alcance indicado
object BudgetSpentCalculator {
    fun spent(transactions: List<Transaction>, categoryId: Long?): Long =
        MoneyMath.sum(
            transactions
                .filter { it.type == TransactionType.EXPENSE && (categoryId == null || it.categoryId == categoryId) }
                .map { it.amountCents },
        )
}
