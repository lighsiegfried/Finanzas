package com.kratt.finanzas.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kratt.finanzas.MainActivity
import com.kratt.finanzas.R
import com.kratt.finanzas.navigation.ShortcutRouting

// widget de acciones rapidas; abre los flujos existentes, no guarda movimientos
class QuickActionsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background).padding(12.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = context.getString(R.string.widget_quick_actions_title),
                        style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    ActionRow(context, R.string.shortcut_add_expense, ShortcutRouting.ACTION_ADD_EXPENSE)
                    Spacer(GlanceModifier.height(6.dp))
                    ActionRow(context, R.string.shortcut_add_income, ShortcutRouting.ACTION_ADD_INCOME)
                    Spacer(GlanceModifier.height(6.dp))
                    ActionRow(context, R.string.shortcut_transfer, ShortcutRouting.ACTION_TRANSFER)
                }
            }
        }
    }
}

// boton que lanza la app con la accion indicada; el flujo real pide autenticacion si esta bloqueada
@Composable
private fun ActionRow(context: Context, labelRes: Int, action: String) {
    val label = context.getString(labelRes)
    val intent = Intent(context, MainActivity::class.java)
        .setAction(action)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    Text(
        text = label,
        style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 15.sp, fontWeight = FontWeight.Medium),
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clickable(actionStartActivity(intent))
            .semantics { contentDescription = label },
    )
}

// receptor del widget de acciones rapidas
class QuickActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickActionsWidget()
}
