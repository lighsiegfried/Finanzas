package com.kratt.finanzas.presentation.report

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.common.CurrencyFormatter
import com.kratt.finanzas.common.MonthFormatter
import com.kratt.finanzas.data.report.CsvCell
import com.kratt.finanzas.data.preferences.DisplayPreferences
import com.kratt.finanzas.data.report.CsvExporter
import com.kratt.finanzas.data.report.CsvTable
import com.kratt.finanzas.data.repository.BudgetRepository
import com.kratt.finanzas.data.repository.InstallmentRepository
import com.kratt.finanzas.data.repository.RecurringRepository
import com.kratt.finanzas.data.repository.ReportRepository
import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus
import com.kratt.finanzas.domain.model.ReportViewMode
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.BudgetCalculator
import com.kratt.finanzas.domain.usecase.MonthRange
import com.kratt.finanzas.domain.usecase.MonthlyComparison
import com.kratt.finanzas.domain.usecase.OccurrenceView
import com.kratt.finanzas.domain.usecase.InstallmentProgressCalculator
import com.kratt.finanzas.domain.usecase.MoneyMath
import com.kratt.finanzas.domain.usecase.ReportPeriod
import com.kratt.finanzas.domain.usecase.ReportPeriods
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ReportType {
    INCOME_EXPENSE, BY_CATEGORY, BY_ACCOUNT, TREND, ACCOUNT_SUMMARY,
    CREDIT_DEBT, INSTALLMENT_COMMITMENTS, RECURRING_COMMITMENTS, BUDGET_PERFORMANCE, COMPARISON,
    // reportes de ahorro y compras planificadas (fase 5c)
    SAVINGS_PROGRESS, CONTRIBUTIONS_BY_MONTH, ACTIVE_GOALS, COMPLETED_GOALS, PLANNED_PURCHASES_REPORT, PLANNED_BY_PRIORITY,
}

enum class ReportChart { NONE, COLUMN, LINE }

// filtro de movimientos asociado a un punto de la grafica, para abrir el historial ya filtrado
data class ReportChartFilter(
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val type: TransactionType? = null,
)

data class ReportRender(
    val chartType: ReportChart = ReportChart.NONE,
    val chartValues: List<Double> = emptyList(),
    val chartLabels: List<String> = emptyList(),
    // montos reales en centavos por punto; el detalle nunca sale de las coordenadas de la grafica
    val chartAmountsCents: List<Long> = emptyList(),
    // filtro de movimientos por punto, alineado con chartLabels; vacio si el reporte no tiene filtro natural
    val chartFilters: List<ReportChartFilter> = emptyList(),
    val summary: String = "",
    val tableHeader: List<String> = emptyList(),
    val tableRows: List<List<String>> = emptyList(),
    val csv: CsvTable? = null,
)

enum class ReportExport { SUCCESS, ERROR }

data class ReportUiState(
    val isLoading: Boolean = true,
    val period: ReportPeriod = ReportPeriod.THIS_MONTH,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
    val render: ReportRender = ReportRender(),
)

// filtro interno del reporte: periodo mas rango personalizado opcional
private data class ReportFilter(
    val period: ReportPeriod = ReportPeriod.THIS_MONTH,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(
    private val appContext: Context,
    private val reportRepository: ReportRepository,
    private val budgetRepository: BudgetRepository,
    private val installmentRepository: InstallmentRepository,
    private val recurringRepository: RecurringRepository,
    private val savingsGoalRepository: com.kratt.finanzas.data.repository.SavingsGoalRepository,
    private val savingsContributionRepository: com.kratt.finanzas.data.repository.SavingsContributionRepository,
    private val plannedPurchaseRepository: com.kratt.finanzas.data.repository.PlannedPurchaseRepository,
    private val csvExporter: CsvExporter,
    private val displayPreferences: DisplayPreferences,
    private val type: ReportType,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val filter = MutableStateFlow(ReportFilter())

    // modo de vista del reporte: grafica, lista o ambas; se guarda en preferencias
    val reportViewMode: StateFlow<ReportViewMode> = displayPreferences.settings
        .map { it.reportViewMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportViewMode.BOTH)

    fun onReportViewMode(mode: ReportViewMode) {
        viewModelScope.launch { displayPreferences.setReportViewMode(mode) }
    }

    val uiState: StateFlow<ReportUiState> = filter
        .mapLatest { f -> ReportUiState(isLoading = false, period = f.period, customStart = f.customStart, customEnd = f.customEnd, render = load(f)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    private val _export = MutableStateFlow<ReportExport?>(null)
    val exportStatus: StateFlow<ReportExport?> = _export.asStateFlow()

    fun onPeriod(p: ReportPeriod) { filter.update { it.copy(period = p) } }
    // aplica un rango personalizado solo si es valido, evita reportes con fechas al reves
    fun onCustomRange(start: LocalDate, end: LocalDate) {
        if (!start.isAfter(end)) filter.value = ReportFilter(ReportPeriod.CUSTOM, start, end)
    }
    fun currentCsv(): CsvTable? = uiState.value.render.csv

    // escribe el csv en el destino elegido por el usuario, sin bloquear la ui
    fun onExport(uri: Uri, resolver: ContentResolver) {
        val table = currentCsv() ?: return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { resolver.openOutputStream(uri)?.use { csvExporter.write(it, table) } != null }.getOrDefault(false)
            }
            _export.value = if (ok) ReportExport.SUCCESS else ReportExport.ERROR
        }
    }

    fun onExportShown() { _export.value = null }

    private fun s(id: Int): String = appContext.getString(id)
    private fun fmt(cents: Long): String = CurrencyFormatter.format(cents)
    private fun machine(cents: Long): String = AmountParser.formatCents(cents)
    private fun money(cents: Long): Double = cents / 100.0

    private suspend fun load(f: ReportFilter): ReportRender {
        val range = ReportPeriods.range(f.period, today, f.customStart, f.customEnd)
        return when (type) {
            ReportType.INCOME_EXPENSE -> incomeExpense(range)
            ReportType.BY_CATEGORY -> byCategory(range)
            ReportType.BY_ACCOUNT -> byAccount(range)
            ReportType.TREND -> trend()
            ReportType.ACCOUNT_SUMMARY -> accountSummary(range)
            ReportType.CREDIT_DEBT -> creditDebt()
            ReportType.INSTALLMENT_COMMITMENTS -> installmentCommitments()
            ReportType.RECURRING_COMMITMENTS -> recurringCommitments()
            ReportType.BUDGET_PERFORMANCE -> budgetPerformance(range.start)
            ReportType.COMPARISON -> comparison(range.start)
            ReportType.SAVINGS_PROGRESS -> savingsProgress()
            ReportType.CONTRIBUTIONS_BY_MONTH -> contributionsByMonth(range)
            ReportType.ACTIVE_GOALS -> goalsByStatus(active = true)
            ReportType.COMPLETED_GOALS -> goalsByStatus(active = false)
            ReportType.PLANNED_PURCHASES_REPORT -> plannedPurchasesReport()
            ReportType.PLANNED_BY_PRIORITY -> plannedByPriority()
        }
    }

    private suspend fun incomeExpense(range: com.kratt.finanzas.domain.usecase.DateRange): ReportRender {
        val ie = reportRepository.incomeExpense(range)
        val header = listOf(s(R.string.report_income_expense), s(R.string.report_total))
        val rows = listOf(
            listOf(s(R.string.income_label), fmt(ie.incomeCents)),
            listOf(s(R.string.expense_label), fmt(ie.expenseCents)),
            listOf(s(R.string.monthly_balance_label), fmt(ie.netCents)),
        )
        return ReportRender(
            chartType = ReportChart.COLUMN,
            chartValues = listOf(money(ie.incomeCents), money(ie.expenseCents)),
            chartLabels = listOf(s(R.string.income_label), s(R.string.expense_label)),
            chartAmountsCents = listOf(ie.incomeCents, ie.expenseCents),
            chartFilters = listOf(ReportChartFilter(type = TransactionType.INCOME), ReportChartFilter(type = TransactionType.EXPENSE)),
            summary = "${s(R.string.income_label)}: ${fmt(ie.incomeCents)}. ${s(R.string.expense_label)}: ${fmt(ie.expenseCents)}. ${s(R.string.monthly_balance_label)}: ${fmt(ie.netCents)}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, rows.map { listOf(CsvCell.Text(it[0]), CsvCell.Text(it[1])) }),
        )
    }

    private suspend fun byCategory(range: com.kratt.finanzas.domain.usecase.DateRange): ReportRender {
        val totals = reportRepository.expensesByCategory(range)
        val totalExpense = totals.sumOf { it.totalCents }
        val header = listOf(s(R.string.category_label), s(R.string.report_total), s(R.string.report_percentage), s(R.string.report_movements))
        val rows = totals.map { row ->
            val pct = if (totalExpense > 0) (row.totalCents * 100 / totalExpense).toInt() else 0
            listOf(row.name, fmt(row.totalCents), "$pct%", row.movementCount.toString())
        }
        return ReportRender(
            chartType = ReportChart.COLUMN,
            chartValues = totals.map { money(it.totalCents) },
            chartLabels = totals.map { it.name },
            chartAmountsCents = totals.map { it.totalCents },
            chartFilters = totals.map { ReportChartFilter(categoryId = it.id, type = TransactionType.EXPENSE) },
            summary = if (totals.isEmpty()) s(R.string.chart_empty) else "${s(R.string.report_by_category)}: ${totals.joinToString(", ") { "${it.name} ${fmt(it.totalCents)}" }}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, totals.map { row ->
                listOf(CsvCell.Text(row.name), CsvCell.Value(machine(row.totalCents)), CsvCell.Value(if (totalExpense > 0) "${row.totalCents * 100 / totalExpense}" else "0"), CsvCell.Value(row.movementCount.toString()))
            }),
        )
    }

    private suspend fun byAccount(range: com.kratt.finanzas.domain.usecase.DateRange): ReportRender {
        val totals = reportRepository.expensesByAccount(range)
        val header = listOf(s(R.string.account_label), s(R.string.report_total), s(R.string.report_movements))
        val rows = totals.map { listOf(it.name, fmt(it.totalCents), it.movementCount.toString()) }
        return ReportRender(
            chartType = ReportChart.COLUMN, chartValues = totals.map { money(it.totalCents) }, chartLabels = totals.map { it.name },
            chartAmountsCents = totals.map { it.totalCents },
            chartFilters = totals.map { ReportChartFilter(accountId = it.id, type = TransactionType.EXPENSE) },
            summary = if (totals.isEmpty()) s(R.string.chart_empty) else "${s(R.string.report_by_account)}: ${totals.joinToString(", ") { "${it.name} ${fmt(it.totalCents)}" }}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, totals.map { listOf(CsvCell.Text(it.name), CsvCell.Value(machine(it.totalCents)), CsvCell.Value(it.movementCount.toString())) }),
        )
    }

    private suspend fun trend(): ReportRender {
        val points = reportRepository.monthlyTrend(YearMonth.from(today), 6)
        val header = listOf(s(R.string.period_label), s(R.string.income_label), s(R.string.expense_label), s(R.string.monthly_balance_label))
        val rows = points.map { p ->
            val label = MonthFormatter.format(YearMonth.of(p.year, p.month))
            listOf(label, fmt(p.incomeCents), fmt(p.expenseCents), fmt(p.balanceCents))
        }
        return ReportRender(
            chartType = ReportChart.LINE, chartValues = points.map { money(it.balanceCents) },
            chartLabels = points.map { MonthFormatter.format(YearMonth.of(it.year, it.month)) },
            chartAmountsCents = points.map { it.balanceCents },
            summary = "${s(R.string.report_trend)}: ${points.joinToString(", ") { "${MonthFormatter.format(YearMonth.of(it.year, it.month))} ${fmt(it.balanceCents)}" }}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, points.map { p -> listOf(CsvCell.Text(MonthFormatter.format(YearMonth.of(p.year, p.month))), CsvCell.Value(machine(p.incomeCents)), CsvCell.Value(machine(p.expenseCents)), CsvCell.Value(machine(p.balanceCents))) }),
        )
    }

    private suspend fun accountSummary(range: com.kratt.finanzas.domain.usecase.DateRange): ReportRender {
        val accounts = reportRepository.accountReport(range)
        val header = listOf(s(R.string.account_label), s(R.string.account_opening), s(R.string.income_label), s(R.string.expense_label), s(R.string.account_closing))
        val rows = accounts.map { listOf(it.name, fmt(it.openingCents), fmt(it.incomeCents), fmt(it.expenseCents), fmt(it.closingCents)) }
        return ReportRender(
            chartType = ReportChart.COLUMN, chartValues = accounts.map { money(it.closingCents) }, chartLabels = accounts.map { it.name },
            chartAmountsCents = accounts.map { it.closingCents },
            summary = "${s(R.string.report_account_summary)}: ${accounts.joinToString(", ") { "${it.name} ${fmt(it.closingCents)}" }}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, accounts.map { listOf(CsvCell.Text(it.name), CsvCell.Value(machine(it.openingCents)), CsvCell.Value(machine(it.incomeCents)), CsvCell.Value(machine(it.expenseCents)), CsvCell.Value(machine(it.closingCents))) }),
        )
    }

    private suspend fun creditDebt(): ReportRender {
        val cards = reportRepository.creditCardDebt()
        val header = listOf(s(R.string.account_label), s(R.string.balance_debt), s(R.string.balance_available))
        val rows = cards.map { (name, balance) ->
            listOf(name, fmt(balance.debtCents), balance.availableCreditCents?.let { fmt(it) } ?: s(R.string.no_credit_limit))
        }
        return ReportRender(
            chartType = ReportChart.COLUMN, chartValues = cards.map { money(it.second.debtCents) }, chartLabels = cards.map { it.first },
            chartAmountsCents = cards.map { it.second.debtCents },
            summary = if (cards.isEmpty()) s(R.string.chart_empty) else "${s(R.string.report_credit_debt)}: ${cards.joinToString(", ") { "${it.first} ${fmt(it.second.debtCents)}" }}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, cards.map { (name, b) -> listOf(CsvCell.Text(name), CsvCell.Value(machine(b.debtCents)), CsvCell.Value(b.availableCreditCents?.let { machine(it) } ?: "")) }),
        )
    }

    private suspend fun installmentCommitments(): ReportRender {
        val plans = installmentRepository.observePlans().first()
        val header = listOf(s(R.string.installment_name_label), s(R.string.total_amount_label), s(R.string.paid_installments), s(R.string.pending_installments), s(R.string.next_payment))
        val rows = plans.map { plan ->
            val occ = installmentRepository.occurrencesForPlan(plan.id)
            val progress = InstallmentProgressCalculator.calculate(occ.map { OccurrenceView(it.amountCents, it.status) })
            val next = occ.filter { it.status == InstallmentOccurrenceStatus.PENDING || it.status == InstallmentOccurrenceStatus.OVERDUE }.minByOrNull { it.dueDate }?.dueDate
            listOf(plan.name, fmt(plan.totalAmountCents), progress.paidCount.toString(), progress.pendingCount.toString(), next?.toString() ?: "-")
        }
        return ReportRender(
            chartType = ReportChart.NONE,
            summary = "${s(R.string.report_installment_commitments)}: ${plans.size}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, plans.map { plan ->
                val occ = installmentRepository.occurrencesForPlan(plan.id)
                val progress = InstallmentProgressCalculator.calculate(occ.map { OccurrenceView(it.amountCents, it.status) })
                listOf(CsvCell.Text(plan.name), CsvCell.Value(machine(plan.totalAmountCents)), CsvCell.Value(progress.paidCount.toString()), CsvCell.Value(progress.pendingCount.toString()))
            }),
        )
    }

    private suspend fun recurringCommitments(): ReportRender {
        val templates = recurringRepository.observeTemplates().first()
        val header = listOf(s(R.string.name_label), s(R.string.amount_label), s(R.string.frequency_label))
        val rows = templates.map { listOf(it.name, fmt(it.amountCents), it.recurrenceType.name) }
        return ReportRender(
            chartType = ReportChart.NONE,
            summary = "${s(R.string.report_recurring)}: ${templates.size}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, templates.map { listOf(CsvCell.Text(it.name), CsvCell.Value(machine(it.amountCents)), CsvCell.Value(it.recurrenceType.name)) }),
        )
    }

    private suspend fun budgetPerformance(anchor: LocalDate): ReportRender {
        val month = YearMonth.from(anchor)
        val range = MonthRange.of(month)
        val budgets = budgetRepository.observeForMonth(month.year, month.monthValue).first()
        val header = listOf(s(R.string.category_label), s(R.string.limit_label), s(R.string.spent_label), s(R.string.remaining_label), s(R.string.period_label))
        val rows = budgets.map { b ->
            val spent = if (b.isOverall) reportRepository.incomeExpense(com.kratt.finanzas.domain.usecase.DateRange(month.atDay(1), month.atEndOfMonth())).expenseCents
            else reportRepositorySpentCategory(b.categoryId!!, range)
            val progress = BudgetCalculator.progress(b.limitAmountCents, spent, b.warningPercentage)
            val stateRes = when (progress.state) {
                com.kratt.finanzas.domain.usecase.BudgetState.AVAILABLE -> R.string.balance_available
                com.kratt.finanzas.domain.usecase.BudgetState.WARNING -> R.string.budget_state_warning
                com.kratt.finanzas.domain.usecase.BudgetState.EXCEEDED -> R.string.budget_state_exceeded
            }
            listOf(if (b.isOverall) s(R.string.budget_overall) else "#${b.categoryId}", fmt(b.limitAmountCents), fmt(spent), fmt(progress.remainingCents), s(stateRes))
        }
        return ReportRender(
            chartType = ReportChart.NONE,
            summary = "${s(R.string.report_budget_performance)}: ${budgets.size}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, rows.map { r -> r.map { CsvCell.Text(it) } }),
        )
    }

    private suspend fun reportRepositorySpentCategory(categoryId: Long, range: MonthRange): Long {
        // usa la agregacion sql del reporte para el gasto de la categoria del mes
        val start = LocalDate.ofEpochDay(range.startEpochDay)
        val end = LocalDate.ofEpochDay(range.endEpochDay)
        return reportRepository.expensesByCategory(com.kratt.finanzas.domain.usecase.DateRange(start, end)).firstOrNull { it.id == categoryId }?.totalCents ?: 0L
    }

    private suspend fun comparison(anchor: LocalDate): ReportRender {
        val month = YearMonth.from(anchor)
        val prev = month.minusMonths(1)
        val curRange = com.kratt.finanzas.domain.usecase.DateRange(month.atDay(1), month.atEndOfMonth())
        val prevRange = com.kratt.finanzas.domain.usecase.DateRange(prev.atDay(1), prev.atEndOfMonth())
        val cur = reportRepository.incomeExpense(curRange)
        val previous = reportRepository.incomeExpense(prevRange)
        val incomeChange = MonthlyComparison.compare(previous.incomeCents, cur.incomeCents)
        val expenseChange = MonthlyComparison.compare(previous.expenseCents, cur.expenseCents)
        fun changeText(c: com.kratt.finanzas.domain.usecase.MetricChange): String {
            val dirRes = when (c.direction) {
                com.kratt.finanzas.domain.usecase.ChangeDirection.UP -> R.string.increased
                com.kratt.finanzas.domain.usecase.ChangeDirection.DOWN -> R.string.decreased
                com.kratt.finanzas.domain.usecase.ChangeDirection.SAME -> R.string.no_change
            }
            return if (!c.hasPrevious) s(R.string.no_previous_data) else "${s(dirRes)} ${fmt(c.deltaCents)} (${c.percentAbs}%)"
        }
        val header = listOf(s(R.string.comparison_title), s(R.string.period_last_month), s(R.string.period_this_month))
        val rows = listOf(
            listOf(s(R.string.income_label), fmt(previous.incomeCents), fmt(cur.incomeCents)),
            listOf(s(R.string.expense_label), fmt(previous.expenseCents), fmt(cur.expenseCents)),
            listOf(s(R.string.monthly_balance_label), fmt(previous.netCents), fmt(cur.netCents)),
        )
        return ReportRender(
            chartType = ReportChart.COLUMN,
            chartValues = listOf(money(previous.expenseCents), money(cur.expenseCents)),
            chartLabels = listOf(s(R.string.period_last_month), s(R.string.period_this_month)),
            chartAmountsCents = listOf(previous.expenseCents, cur.expenseCents),
            summary = "${s(R.string.income_label)}: ${changeText(incomeChange)}. ${s(R.string.expense_label)}: ${changeText(expenseChange)}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, rows.map { r -> listOf(CsvCell.Text(r[0]), CsvCell.Text(r[1]), CsvCell.Text(r[2])) }),
        )
    }

    // progreso de ahorro: metas activas con lo ahorrado y su avance
    private suspend fun savingsProgress(): ReportRender {
        val goals = savingsGoalRepository.observeAll().first()
            .filter { it.status == com.kratt.finanzas.domain.model.SavingsGoalStatus.ACTIVE && !it.isArchived }
        val totals = savingsGoalRepository.observeTotalsByGoal().first().associate { it.savingsGoalId to it.totalCents }
        val header = listOf(s(R.string.goal_name_label), s(R.string.goal_target_label), s(R.string.goal_saved), s(R.string.goal_progress))
        val rows = goals.map { g ->
            val c = totals[g.id] ?: 0L
            val pct = if (g.targetAmountCents > 0) (c * 100 / g.targetAmountCents).toInt() else 0
            listOf(g.name, fmt(g.targetAmountCents), fmt(c), "$pct%")
        }
        return ReportRender(
            chartType = ReportChart.COLUMN,
            chartValues = goals.map { money(totals[it.id] ?: 0L) },
            chartLabels = goals.map { it.name },
            chartAmountsCents = goals.map { totals[it.id] ?: 0L },
            summary = if (goals.isEmpty()) s(R.string.chart_empty) else "${s(R.string.report_savings_progress)}: ${goals.joinToString(", ") { "${it.name} ${fmt(totals[it.id] ?: 0L)}" }}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, rows.map { r -> r.map { CsvCell.Text(it) } }),
        )
    }

    // aportes agrupados por mes dentro del periodo elegido
    private suspend fun contributionsByMonth(range: com.kratt.finanzas.domain.usecase.DateRange): ReportRender {
        val contribs = savingsContributionRepository.listBetween(range.start, range.end)
        val byMonth = contribs.groupBy { YearMonth.from(it.contributionDate) }
            .mapValues { entry -> MoneyMath.sum(entry.value.map { it.amountCents }) }
            .toSortedMap()
        val header = listOf(s(R.string.period_label), s(R.string.contribution_amount))
        val rows = byMonth.map { (ym, total) -> listOf(MonthFormatter.format(ym), fmt(total)) }
        return ReportRender(
            chartType = ReportChart.LINE,
            chartValues = byMonth.values.map { money(it) },
            chartLabels = byMonth.keys.map { MonthFormatter.format(it) },
            chartAmountsCents = byMonth.values.toList(),
            summary = if (rows.isEmpty()) s(R.string.chart_empty) else "${s(R.string.report_contributions_by_month)}: ${byMonth.entries.joinToString(", ") { "${MonthFormatter.format(it.key)} ${fmt(it.value)}" }}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, rows.map { r -> r.map { CsvCell.Text(it) } }),
        )
    }

    // lista de metas activas o completadas segun se pida
    private suspend fun goalsByStatus(active: Boolean): ReportRender {
        val goals = savingsGoalRepository.observeAll().first().filter {
            if (active) it.status == com.kratt.finanzas.domain.model.SavingsGoalStatus.ACTIVE && !it.isArchived
            else it.status == com.kratt.finanzas.domain.model.SavingsGoalStatus.COMPLETED
        }
        val totals = savingsGoalRepository.observeTotalsByGoal().first().associate { it.savingsGoalId to it.totalCents }
        val header = listOf(s(R.string.goal_name_label), s(R.string.goal_target_label), s(R.string.goal_saved))
        val rows = goals.map { g -> listOf(g.name, fmt(g.targetAmountCents), fmt(totals[g.id] ?: 0L)) }
        val titleRes = if (active) R.string.report_active_goals else R.string.report_completed_goals
        return ReportRender(
            chartType = ReportChart.NONE,
            summary = "${s(titleRes)}: ${goals.size}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, rows.map { r -> r.map { CsvCell.Text(it) } }),
        )
    }

    // lista de compras planificadas pendientes
    private suspend fun plannedPurchasesReport(): ReportRender {
        val purchases = plannedPurchaseRepository.observeAll().first()
            .filter { it.status != com.kratt.finanzas.domain.model.PurchaseStatus.PURCHASED && it.status != com.kratt.finanzas.domain.model.PurchaseStatus.CANCELLED }
        val header = listOf(s(R.string.purchase_name), s(R.string.purchase_cost), s(R.string.purchase_priority))
        val rows = purchases.map { p -> listOf(p.name, fmt(p.estimatedCostCents), s(priorityLabel(p.priority))) }
        return ReportRender(
            chartType = ReportChart.NONE,
            summary = "${s(R.string.report_planned_purchases)}: ${purchases.size}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, rows.map { r -> r.map { CsvCell.Text(it) } }),
        )
    }

    // monto total planificado agrupado por prioridad
    private suspend fun plannedByPriority(): ReportRender {
        val purchases = plannedPurchaseRepository.observeAll().first()
            .filter { it.status != com.kratt.finanzas.domain.model.PurchaseStatus.PURCHASED && it.status != com.kratt.finanzas.domain.model.PurchaseStatus.CANCELLED }
        val order = listOf(
            com.kratt.finanzas.domain.model.PurchasePriority.HIGH,
            com.kratt.finanzas.domain.model.PurchasePriority.MEDIUM,
            com.kratt.finanzas.domain.model.PurchasePriority.LOW,
        )
        val byPriority = order.associateWith { pr -> MoneyMath.sum(purchases.filter { it.priority == pr }.map { it.estimatedCostCents }) }
        val header = listOf(s(R.string.purchase_priority), s(R.string.purchase_cost))
        val rows = byPriority.map { (pr, total) -> listOf(s(priorityLabel(pr)), fmt(total)) }
        return ReportRender(
            chartType = ReportChart.COLUMN,
            chartValues = byPriority.values.map { money(it) },
            chartLabels = order.map { s(priorityLabel(it)) },
            chartAmountsCents = byPriority.values.toList(),
            summary = "${s(R.string.report_planned_by_priority)}: ${byPriority.entries.joinToString(", ") { "${s(priorityLabel(it.key))} ${fmt(it.value)}" }}.",
            tableHeader = header, tableRows = rows,
            csv = CsvTable(header, rows.map { r -> r.map { CsvCell.Text(it) } }),
        )
    }

    private fun priorityLabel(priority: com.kratt.finanzas.domain.model.PurchasePriority): Int = when (priority) {
        com.kratt.finanzas.domain.model.PurchasePriority.LOW -> R.string.priority_low
        com.kratt.finanzas.domain.model.PurchasePriority.MEDIUM -> R.string.priority_medium
        com.kratt.finanzas.domain.model.PurchasePriority.HIGH -> R.string.priority_high
    }
}
