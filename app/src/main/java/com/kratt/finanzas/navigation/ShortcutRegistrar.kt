package com.kratt.finanzas.navigation

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.kratt.finanzas.MainActivity
import com.kratt.finanzas.R

// registra los accesos directos dinamicos; usa la clase real de la app, sirve para cualquier variante
object ShortcutRegistrar {

    fun register(context: Context) {
        val shortcuts = listOf(
            build(context, "add_expense", R.string.shortcut_add_expense, ShortcutRouting.ACTION_ADD_EXPENSE),
            build(context, "add_income", R.string.shortcut_add_income, ShortcutRouting.ACTION_ADD_INCOME),
            build(context, "transfer", R.string.shortcut_transfer, ShortcutRouting.ACTION_TRANSFER),
            build(context, "upcoming", R.string.shortcut_upcoming, ShortcutRouting.ACTION_UPCOMING),
        )
        // no debe tumbar el arranque si el launcher rechaza los accesos directos
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }

    // arma un acceso directo que abre la app en la accion indicada, sin montos en la etiqueta
    private fun build(context: Context, id: String, labelRes: Int, action: String): ShortcutInfoCompat {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val label = context.getString(labelRes)
        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_finance_logo))
            .setIntent(intent)
            .build()
    }
}
