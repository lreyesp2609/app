package com.example.app.screen.recordatorios.components

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.app.MainActivity
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

        // 🔥 DESPERTAR DISPOSITIVO INMEDIATAMENTE
        NotificationHelper.wakeUpDevice(context)

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

                if (!reminder.is_active || reminder.is_deleted) {
                    Log.w("ReminderReceiver", "⚠️ Recordatorio inactivo/eliminado")
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
                    Log.d("ReminderReceiver", "🔄 Reprogramando alarma")
                    val tempReminder = reminder.copy(id = alarmId, days = day)
                    scheduleReminder(context, tempReminder)
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

        NotificationHelper.createNotificationChannel(context)

        val title = reminder.title
        val description = reminder.description ?: "Recordatorio"
        val day = intent.getStringExtra("day") ?: ""

        // Intent para pantalla completa
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reminder_id", reminder.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("$description${if (day.isNotEmpty()) " ($day)" else ""}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // 🔥 IMPORTANTE: NO uses DEFAULT_ALL si vas a configurar sonido manualmente
        // En su lugar, configura cada cosa por separado

        // 🔥 Configurar sonido según el tipo seleccionado
        if (reminder.sound) {
            val soundUri = when (reminder.sound_type) {
                "default" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                "gentle" -> {
                    val ringtoneManager = RingtoneManager(context)
                    ringtoneManager.setType(RingtoneManager.TYPE_NOTIFICATION)
                    val cursor = ringtoneManager.cursor
                    if (cursor.moveToPosition(0)) {
                        ringtoneManager.getRingtoneUri(0)
                    } else {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }
                }
                "alert" -> {
                    RingtoneManager.getActualDefaultRingtoneUri(
                        context,
                        RingtoneManager.TYPE_ALARM
                    ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                "chime" -> {
                    val ringtoneManager = RingtoneManager(context)
                    ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE)
                    val cursor = ringtoneManager.cursor
                    if (cursor.moveToPosition(0)) {
                        ringtoneManager.getRingtoneUri(0)
                    } else {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    }
                }
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            notificationBuilder.setSound(soundUri)
            Log.d("ReminderReceiver", "   🔊 Sonido: ${reminder.sound_type} - URI: $soundUri")
        } else {
            Log.d("ReminderReceiver", "   🔇 Sonido: desactivado")
        }

        // 🔥 Configurar vibración
        if (reminder.vibration) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            Log.d("ReminderReceiver", "   📳 Vibración: activada")
        } else {
            Log.d("ReminderReceiver", "   📳 Vibración: desactivada")
        }

        // 🔥 Luces (opcional pero bueno tenerlo)
        notificationBuilder
            .setLights(Color.BLUE, 1000, 1000)
            .setDefaults(0) // 🔥 IMPORTANTE: Sin defaults automáticos

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = Random.nextInt(1000, 9999)
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d("ReminderReceiver", "   ✅ Notificación mostrada con ID: $notificationId")
    }
}