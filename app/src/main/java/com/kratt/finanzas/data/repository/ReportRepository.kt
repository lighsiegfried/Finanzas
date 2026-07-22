package com.kratt.finanzas.data.repository

import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.domain.model.AccountBalance
import com.kratt.finanzas.domain.model.AccountReportRow
import com.kratt.finanzas.domain.model.AccountTotals
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.IncomeExpense
import com.kratt.finanzas.domain.model.LabeledTotal
import com.kratt.finanzas.domain.model.TrendPoint
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.usecase.AccountBalanceCalculator
import com.kratt.finanzas.domain.usecase.DateRange
import com.kratt.finanzas.domain.usecase.MonthRange
import java.time.YearMonth
import kotlinx.coroutines.flow.first

// produce los datos de los reportes usando agregacion sql, sin cargar todo el historial
class ReportRepository(
    database: AppDatabase,
    private val accountRepository: AccountRepository,
) {
    private val dao = database.transactionDao()

    private fun days(range: DateRange): Pair<Long, Long> = range.start.toEpochDay() to range.end.toEpochDay()

    suspend fun incomeExpense(range: DateRange): IncomeExpense {
        val (start, end) = days(range)
        return IncomeExpense(
            incomeCents = dao.sumByTypeBetween("INCOME", start, end),
            expenseCents = dao.sumByTypeBetween("EXPENSE", start, end),
        )
    }

    suspend fun expensesByCategory(range: DateRange): List<LabeledTotal> {
        val (start, end) = days(range)
        return dao.expensesByCategoryBetween(start, end).map { LabeledTotal(it.categoryId, it.name, it.totalCents, it.movementCount) }
    }

    suspend fun expensesByAccount(range: DateRange): List<LabeledTotal> {
        val (start, end) = days(range)
        return dao.expensesByAccountBetween(start, end).map { LabeledTotal(it.accountId, it.name, it.totalCents, it.movementCount) }
    }

    // tendencia de los ultimos meses, una consulta agregada por mes
    suspend fun monthlyTrend(anchor: YearMonth, count: Int): List<TrendPoint> =
        (0 until count).map { index ->
            val month = anchor.minusMonths((count - 1 - index).toLong())
            val range = MonthRange.of(month)
            val income = dao.sumByTypeBetween("INCOME", range.startEpochDay, range.endEpochDay)
            val expense = dao.sumByTypeBetween("EXPENSE", range.startEpochDay, range.endEpochDay)
            TrendPoint(month.year, month.monthValue, income, expense, income - expense)
        }

    // reporte por cuenta con saldo de apertura y cierre del rango
    suspend fun accountReport(range: DateRange): List<AccountReportRow> {
        val (start, end) = days(range)
        return accountRepository.observeAllAccounts().first().map { account ->
            val id = account.id
            val opening = account.initialBalanceCents +
                dao.sumAccountTypeBefore(id, "INCOME", start) -
                dao.sumAccountTypeBefore(id, "EXPENSE", start) +
                dao.sumTransfersIntoBefore(id, start) -
                dao.sumAccountTypeBefore(id, "TRANSFER", start)
            val income = dao.sumAccountTypeBetween(id, "INCOME", start, end)
            val expense = dao.sumAccountTypeBetween(id, "EXPENSE", start, end)
            val transferIn = dao.sumTransfersIntoBetween(id, start, end)
            val transferOut = dao.sumAccountTypeBetween(id, "TRANSFER", start, end)
            AccountReportRow(
                accountId = id, name = account.name, openingCents = opening, incomeCents = income,
                expenseCents = expense, transferInCents = transferIn, transferOutCents = transferOut,
                closingCents = opening + income - expense + transferIn - transferOut,
            )
        }
    }

    // deuda de las tarjetas de credito activas, totales por sql sobre todo el historial
    suspend fun creditCardDebt(): List<Pair<String, AccountBalance>> =
        accountRepository.observeActiveAccounts().first()
            .filter { it.type == AccountType.CREDIT_CARD }
            .map { account ->
                val id = account.id
                val totals = AccountTotals(
                    incomeCents = dao.sumAccountTypeBetween(id, "INCOME", 0, Long.MAX_VALUE),
                    expenseCents = dao.sumAccountTypeBetween(id, "EXPENSE", 0, Long.MAX_VALUE),
                    transferInCents = dao.sumTransfersIntoBetween(id, 0, Long.MAX_VALUE),
                    transferOutCents = dao.sumAccountTypeBetween(id, "TRANSFER", 0, Long.MAX_VALUE),
                )
                account.name to AccountBalanceCalculator.calculate(account, totals)
            }
}
