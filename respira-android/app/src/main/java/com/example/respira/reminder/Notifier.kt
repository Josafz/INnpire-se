package com.example.respira.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.respira.MainActivity
import com.example.respira.R

object Notifier {
    private const val CHANNEL_ID = "respira_lembrete"
    private const val NOTIFICATION_ID = 1001

    /** A partir do Android 13 é preciso o usuário permitir notificações. Antes disso, sempre liberado. */
    fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(ctx: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Lembrete diário", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Lembrete diário para respirar" }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission") // já checado em hasPermission()
    fun showReminder(ctx: Context) {
        if (!hasPermission(ctx)) return
        ensureChannel(ctx)

        // Tocar na notificação abre o app e já abre a respiração guiada
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_OPEN_SOS, true)
        }
        val pending = PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hora de respirar")
            .setContentText("Reserve 1 minuto. Toque para começar a respiração guiada.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
    }
}
