package com.kratt.finanzas.widget

import android.content.Context
import com.kratt.finanzas.FinanzasApplication
import com.kratt.finanzas.common.MonthFormatter
import com.kratt.finanzas.di.DatabaseBootstrapState
import com.kratt.finanzas.domain.usecase.DueClassifier
import com.kratt.finanzas.domain.usecase.DueWhen
import com.kratt.finanzas.domain.usecase.MonthRange
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first

// estado del widget de resumen; los montos viajan como texto ya formateado o enmascarado
sealed interface SummaryWidgetState {
    data object NotReady : SummaryWidgetState
    data class Ready(
        val monthLabel: String,
        val income: String,
        val expense: String,
        val balance: String,
        val hasMovements: Boolean,
        val amountsHidden: Boolean,
    ) : SummaryWidgetState
}

// estado del widget de proximos pagos; solo cuenta, sin montos ni cuentas
sealed interface UpcomingWidgetState {
    data object NotReady : UpcomingWidgetState
    data class Ready(val pendingCount: Int, val overdueCount: Int) : UpcomingWidgetState
}

// lee los datos de los widgets desde los repositorios existentes; nunca calcula dinero aqui
object WidgetContent {

    private fun containerOrNull(context: Context) =
        (context.applicationContext as? FinanzasApplication)?.container

    // resuelve si se pueden mostrar montos: preferencia del widget y ademas la privacidad de la app apagada
    private suspend fun amountsVisible(context: Context): Boolean {
        val container = containerOrNull(context) ?: return false
        val widgetAllows = container.widgetPreferences.showAmountsNow()
        val appHides = container.displayPreferences.settings.first().balancesHidden
        return widgetAllows && !appHides
    }

    suspend fun loadSummary(context: Context, today: LocalDate = LocalDate.now()): SummaryWidgetState {
        val container = containerOrNull(context) ?: return SummaryWidgetState.NotReady
        if (container.databaseState.value != DatabaseBootstrapState.READY) return SummaryWidgetState.NotReady
        return runCatching {
            val month = YearMonth.from(today)
            val summary = container.observeMonthlySummary(month).first()
            val items = container.transactionRepository.observeMonthlyWithNames(month).first()
            val show = amountsVisible(context)
            SummaryWidgetState.Ready(
                monthLabel = MonthFormatter.format(month),
                income = WidgetFormat.amount(summary.incomeCents, show),
                expense = WidgetFormat.amount(summary.expenseCents, show),
                balance = WidgetFormat.amount(summary.balanceCents, show),
                hasMovements = items.isNotEmpty(),
                amountsHidden = !show,
            )
        }.getOrDefault(SummaryWidgetState.NotReady)
    }

    suspend fun loadUpcoming(context: Context, today: LocalDate = LocalDate.now()): UpcomingWidgetState {
        val container = containerOrNull(context) ?: return UpcomingWidgetState.NotReady
        if (container.databaseState.value != DatabaseBootstrapState.READY) return UpcomingWidgetState.NotReady
        return runCatching {
            val month = YearMonth.from(today)
            val range = MonthRange.of(month)
            val commitments = container.commitmentService.dueCommitments(
                LocalDate.ofEpochDay(range.startEpochDay),
                LocalDate.ofEpochDay(range.endEpochDay),
            )
            val overdue = commitments.count { DueClassifier.classify(it.dueDate, today) == DueWhen.OVERDUE }
            UpcomingWidgetState.Ready(pendingCount = commitments.size, overdueCount = overdue)
        }.getOrDefault(UpcomingWidgetState.NotReady)
    }
}
