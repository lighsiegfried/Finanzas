package com.kratt.finanzas.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.kratt.finanzas.MainActivity
import com.kratt.finanzas.R
import com.kratt.finanzas.navigation.ShortcutRouting

// widget de resumen del mes; los montos vienen ya formateados o enmascarados desde el dominio
class SummaryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WidgetContent.loadSummary(context)
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background).padding(12.dp),
                ) {
                    Text(
                        text = context.getString(R.string.widget_summary_title),
                        style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.clickable(openAction(context, ShortcutRouting.ACTION_OPEN_SUMMARY)),
                    )
                    Spacer(GlanceModifier.height(6.dp))
                    when (state) {
                        SummaryWidgetState.NotReady -> Message(context.getString(R.string.widget_open_app_to_update))
                        is SummaryWidgetState.Ready -> ReadyContent(context, state)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyContent(context: Context, state: SummaryWidgetState.Ready) {
    Text(text = state.monthLabel, style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 12.sp))
    if (!state.hasMovements) {
        Spacer(GlanceModifier.height(6.dp))
        Message(context.getString(R.string.widget_no_movements))
        return
    }
    Spacer(GlanceModifier.height(6.dp))
    // ingresos y gastos abren pantallas concretas; el signo y la etiqueta evitan depender del color
    AmountRow(context, context.getString(R.string.income_label), state.income, openAction(context, ShortcutRouting.ACTION_OPEN_SUMMARY))
    AmountRow(context, context.getString(R.string.expense_label), state.expense, openAction(context, ShortcutRouting.ACTION_OPEN_EXPENSES))
    AmountRow(context, context.getString(R.string.monthly_balance_label), state.balance, openAction(context, ShortcutRouting.ACTION_OPEN_BUDGETS))
    if (state.amountsHidden) {
        Spacer(GlanceModifier.height(4.dp))
        Text(text = context.getString(R.string.balances_hidden_label), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
    }
}

@Composable
private fun AmountRow(context: Context, label: String, amount: String, action: androidx.glance.action.Action) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp).clickable(action),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp), modifier = GlanceModifier.defaultWeight())
        Text(text = amount, style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun Message(text: String) {
    Text(text = text, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
}

// arma la accion que abre la app en una pantalla concreta; el flujo respeta el bloqueo
private fun openAction(context: Context, action: String) = actionStartActivity(
    Intent(context, MainActivity::class.java).setAction(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
)

class SummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummaryWidget()
}
