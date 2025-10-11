package com.example.app.screen.recordatorios.components

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.app.utils.NotificationHelper
import kotlin.random.Random

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "🔔 Recordatorio recibido")

        // Asegurarse de que el canal existe
        NotificationHelper.createNotificationChannel(context)

        val title = intent.getStringExtra("title") ?: "Recordatorio"
        val description = intent.getStringExtra("description") ?: ""
        val vibration = intent.getBooleanExtra("vibration", false)
        val sound = intent.getBooleanExtra("sound", false)

        val notificationBuilder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(0)

        if (sound) {
            notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        if (vibration) {
            notificationBuilder.setVibrate(longArrayOf(0, 400, 200, 400))
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = Random.nextInt()
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d("ReminderReceiver", "✅ Notificación mostrada con ID: $notificationId")
    }
}