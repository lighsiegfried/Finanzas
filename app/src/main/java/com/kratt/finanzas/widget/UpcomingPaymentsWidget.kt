package com.kratt.finanzas.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.kratt.finanzas.MainActivity
import com.kratt.finanzas.R
import com.kratt.finanzas.navigation.ShortcutRouting

// widget de proximos pagos; solo mensajes genericos, sin montos ni nombres de cuentas
class UpcomingPaymentsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WidgetContent.loadUpcoming(context)
        val message = when (state) {
            UpcomingWidgetState.NotReady -> context.getString(R.string.widget_open_app_to_update)
            is UpcomingWidgetState.Ready -> messageFor(context, state)
        }
        val action = actionStartActivity(
            Intent(context, MainActivity::class.java).setAction(ShortcutRouting.ACTION_UPCOMING)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background).padding(12.dp).clickable(action),
                ) {
                    Text(
                        text = context.getString(R.string.widget_upcoming_title),
                        style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    Text(text = message, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp))
                }
            }
        }
    }

    // elige un mensaje generico segun cuantos pagos hay, priorizando los atrasados
    private fun messageFor(context: Context, state: UpcomingWidgetState.Ready): String = when {
        state.pendingCount == 0 && state.overdueCount == 0 -> context.getString(R.string.widget_no_upcoming)
        state.overdueCount > 0 -> context.getString(R.string.widget_payment_overdue)
        state.pendingCount == 1 -> context.getString(R.string.widget_payment_upcoming)
        else -> context.getString(R.string.widget_payments_pending_format, state.pendingCount)
    }
}

class UpcomingPaymentsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingPaymentsWidget()
}
