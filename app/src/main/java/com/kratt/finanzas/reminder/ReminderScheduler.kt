package com.kratt.finanzas.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.ZoneId
import java.util.concurrent.TimeUnit

// programa el trabajo local diario, sin alarmas exactas ni servicios remotos
object ReminderScheduler {

    private const val WORK_NAME = "payment_reminders_daily"

    // agenda el trabajo diario para que corra cerca de la hora elegida por el usuario
    fun schedule(context: Context, hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()) {
        val delay = ReminderScheduleCalculator.initialDelayMillis(nowMillis, ZoneId.systemDefault(), hour, minute)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        // update reemplaza el trabajo existente, asi no se duplican los workers
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
