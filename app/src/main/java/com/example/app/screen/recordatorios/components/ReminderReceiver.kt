package com.example.app.screen.recordatorios.components

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.app.models.ReminderEntity
import com.example.app.network.AppDatabase
import com.example.app.utils.NotificationHelper
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "🔔 Recordatorio recibido")

        val alarmId = intent.getIntExtra("reminder_id", -1)

        val parentId = alarmId / 100

        Log.d("ReminderReceiver", "   Alarm ID: $alarmId")
        Log.d("ReminderReceiver", "   Parent ID: $parentId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)

                val reminder = database.reminderDao().getReminderById(parentId)

                if (reminder == null) {
                    Log.w("ReminderReceiver", "⚠️ Recordatorio no encontrado: ParentID=$parentId")
                    return@launch
                }

                if (!reminder.is_active) {
                    Log.w("ReminderReceiver", "⚠️ Recordatorio desactivado: ID=$parentId")
                    return@launch
                }

                if (reminder.is_deleted) {
                    Log.w("ReminderReceiver", "⚠️ Recordatorio eliminado: ID=$parentId")
                    return@launch
                }

                Log.d("ReminderReceiver", "✅ Recordatorio encontrado: ${reminder.title}")

                // Continuar con la notificación
                withContext(Dispatchers.Main) {
                    showNotification(context, intent, reminder)
                }

                // Reprogramar para la próxima semana
                val day = intent.getStringExtra("day")
                val time = intent.getStringExtra("time")

                if (day != null && time != null) {
                    Log.d("ReminderReceiver", "🔄 Reprogramando alarma:")
                    Log.d("ReminderReceiver", "   Día: $day")
                    Log.d("ReminderReceiver", "   Hora: $time")
                    Log.d("ReminderReceiver", "   Alarm ID: $alarmId")

                    // Crear un reminder temporal con el mismo ID de alarma para reprogramar
                    val tempReminder = reminder.copy(
                        id = alarmId,  // ← Mantener el mismo ID de alarma
                        days = day
                    )

                    scheduleReminder(context, tempReminder)
                    Log.d("ReminderReceiver", "✅ Alarma reprogramada para próxima semana")
                }

            } catch (e: Exception) {
                Log.e("ReminderReceiver", "❌ Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun showNotification(context: Context, intent: Intent, reminder: ReminderEntity) {
        Log.d("ReminderReceiver", "📢 MOSTRANDO NOTIFICACIÓN:")
        Log.d("ReminderReceiver", "   Título: ${reminder.title}")
        Log.d("ReminderReceiver", "   Descripción: ${reminder.description}")

        NotificationHelper.createNotificationChannel(context)

        val title = reminder.title
        val description = reminder.description ?: "Recordatorio"
        val day = intent.getStringExtra("day") ?: ""

        val notificationBuilder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("$description${if (day.isNotEmpty()) " ($day)" else ""}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(0)

        if (reminder.sound) {
            notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            Log.d("ReminderReceiver", "   🔊 Sonido: activado")
        }

        if (reminder.vibration) {
            notificationBuilder.setVibrate(longArrayOf(0, 400, 200, 400))
            Log.d("ReminderReceiver", "   📳 Vibración: activada")
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = Random.nextInt(1000, 9999)
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d("ReminderReceiver", "   ✅ Notificación mostrada con ID: $notificationId")
    }
}