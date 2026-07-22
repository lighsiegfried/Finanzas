package com.kratt.finanzas.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kratt.finanzas.R

// publica recordatorios genericos, nunca montos ni cuentas en la pantalla de bloqueo
object ReminderNotifier {

    const val CHANNEL_ID = "payment_reminders"
    private const val NOTIFICATION_ID = 4201
    private const val GOAL_NOTIFICATION_ID = 4202
    private const val PURCHASE_NOTIFICATION_ID = 4203

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }

    // muestra un aviso generico si las notificaciones estan habilitadas
    fun notifyUpcoming(context: Context) {
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.notification_reminder_title))
            .setContentText(context.getString(R.string.notification_reminder_content))
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // sin permiso de notificaciones no se avisa, las finanzas siguen funcionando
        }
    }

    // aviso generico de una meta de ahorro; nunca montos en la pantalla de bloqueo
    fun notifyGoal(context: Context) =
        notifyGeneric(context, GOAL_NOTIFICATION_ID, R.string.reminder_goal_title, R.string.reminder_goal_content)

    // aviso generico de una compra planificada proxima
    fun notifyPurchase(context: Context) =
        notifyGeneric(context, PURCHASE_NOTIFICATION_ID, R.string.reminder_purchase_title, R.string.reminder_purchase_content)

    private fun notifyGeneric(context: Context, id: Int, titleRes: Int, contentRes: Int) {
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(contentRes))
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            // sin permiso de notificaciones no se avisa
        }
    }
}
