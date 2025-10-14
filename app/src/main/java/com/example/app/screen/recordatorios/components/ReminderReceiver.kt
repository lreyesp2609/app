package com.example.app.screen.recordatorios.components

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.app.network.AppDatabase
import com.example.app.utils.NotificationHelper
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "🔔 Recordatorio recibido")

        // Asegurarse de que el canal existe
        NotificationHelper.createNotificationChannel(context)

        // Extraer datos del intent
        val reminderId = intent.getIntExtra("reminder_id", -1)
        val title = intent.getStringExtra("title") ?: "Recordatorio"
        val description = intent.getStringExtra("description") ?: ""
        val vibration = intent.getBooleanExtra("vibration", false)
        val sound = intent.getBooleanExtra("sound", false)
        val day = intent.getStringExtra("day")
        val time = intent.getStringExtra("time")

        Log.d("ReminderReceiver", "📋 Datos del recordatorio:")
        Log.d("ReminderReceiver", "   ID: $reminderId")
        Log.d("ReminderReceiver", "   Título: $title")
        Log.d("ReminderReceiver", "   Día: $day")
        Log.d("ReminderReceiver", "   Hora: $time")

        // 🔔 Mostrar notificación
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

        // 🔄 REPROGRAMAR para la próxima semana (si tiene día y hora)
        if (reminderId != -1 && day != null && time != null) {
            Log.d("ReminderReceiver", "🔄 Reprogramando para la próxima semana...")

            // Usar coroutine para acceder a la base de datos
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val reminder = database.reminderDao().getReminderById(reminderId)

                    if (reminder != null) {
                        // Reprogramar la alarma para la próxima semana
                        scheduleReminder(context, reminder)
                        Log.d("ReminderReceiver", "✅ Recordatorio reprogramado para próxima semana")
                    } else {
                        Log.w("ReminderReceiver", "⚠️ Recordatorio no encontrado en BD: ID=$reminderId")
                    }
                } catch (e: Exception) {
                    Log.e("ReminderReceiver", "❌ Error al reprogramar: ${e.message}")
                    e.printStackTrace()
                }
            }
        } else {
            Log.d("ReminderReceiver", "ℹ️ No se reprograma (recordatorio único o de ubicación)")
        }
    }
}