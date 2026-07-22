package com.kratt.finanzas.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

// punto unico para refrescar todos los widgets tras una operacion exitosa
object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        val app = context.applicationContext
        runCatching { QuickActionsWidget().updateAll(app) }
        runCatching { SummaryWidget().updateAll(app) }
        runCatching { UpcomingPaymentsWidget().updateAll(app) }
    }
}
