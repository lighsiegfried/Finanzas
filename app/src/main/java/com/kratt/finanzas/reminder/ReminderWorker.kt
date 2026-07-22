package com.kratt.finanzas.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kratt.finanzas.FinanzasApplication
import com.kratt.finanzas.domain.usecase.ReminderCalculator
import java.time.LocalDate
import kotlinx.coroutines.flow.first

// trabajo local diario: recalcula compromisos y avisa de pagos proximos, todo sin red
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FinanzasApplication ?: return Result.success()
        val container = app.container
        // si la base no esta lista se omite, no se rompe nada
        if (!container.ensureDatabaseReady()) return Result.success()

        val settings = container.reminderPreferencesRepository.settings.first()
        val today = LocalDate.now()
        val daysBefore = settings.daysBefore

        // recordatorios de pagos: solo si el ajuste global esta activo
        if (settings.enabled) {
            runCatching { container.commitmentService.sync() }
            val commitments = runCatching {
                container.commitmentService.dueCommitments(today.minusDays(35), today.plusDays(daysBefore.toLong()))
            }.getOrDefault(emptyList())
            if (commitments.any { ReminderCalculator.isDueForReminder(it.dueDate, today, daysBefore) }) {
                ReminderNotifier.notifyUpcoming(applicationContext)
            }
        }

        // recordatorios de metas con fecha objetivo proxima, solo las que el usuario activo
        val goalIds = runCatching { container.planningReminderPreferences.enabledGoalIds() }.getOrDefault(emptySet())
        if (goalIds.isNotEmpty()) {
            val goals = runCatching { container.savingsGoalRepository.observeAll().first() }.getOrDefault(emptyList())
            val goalDue = goals.any {
                it.id in goalIds && it.targetDate != null &&
                    it.status == com.kratt.finanzas.domain.model.SavingsGoalStatus.ACTIVE &&
                    ReminderCalculator.isDueForReminder(it.targetDate!!, today, daysBefore)
            }
            if (goalDue) ReminderNotifier.notifyGoal(applicationContext)
        }

        // recordatorios de compras planificadas con fecha proxima que aun no se compran
        val purchaseIds = runCatching { container.planningReminderPreferences.enabledPurchaseIds() }.getOrDefault(emptySet())
        if (purchaseIds.isNotEmpty()) {
            val purchases = runCatching { container.plannedPurchaseRepository.observeAll().first() }.getOrDefault(emptyList())
            val purchaseDue = purchases.any {
                it.id in purchaseIds && it.targetDate != null &&
                    it.status != com.kratt.finanzas.domain.model.PurchaseStatus.PURCHASED &&
                    ReminderCalculator.isDueForReminder(it.targetDate!!, today, daysBefore)
            }
            if (purchaseDue) ReminderNotifier.notifyPurchase(applicationContext)
        }
        return Result.success()
    }
}
