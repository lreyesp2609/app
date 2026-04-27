package com.rutai.app.screen.recordatorios.components

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.rutai.app.MainActivity
import com.rutai.app.models.ReminderEntity
import com.rutai.app.network.AppDatabase
import com.rutai.app.utils.NotificationHelper
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

        val title = reminder.title
        val description = reminder.description ?: "Recordatorio"
        val day = intent.getStringExtra("day") ?: ""

        // 🔥 DETERMINAR QUÉ CANAL USAR SEGÚN LA CONFIGURACIÓN
        val channelId = when {
            // Si tiene sonido personalizado, crear canal con ese sonido
            reminder.sound && !reminder.sound_uri.isNullOrEmpty() -> {
                NotificationHelper.createCustomSoundChannel(context, reminder.sound_uri)
            }
            // Si tiene sonido pero sin URI personalizada, usar canal por defecto
            reminder.sound -> {
                NotificationHelper.CHANNEL_ID
            }
            // Si no tiene sonido, usar canal silencioso
            else -> {
                NotificationHelper.createSilentChannel(context)
            }
        }

        Log.d("ReminderReceiver", "   📡 Canal usado: $channelId")

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

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("$description${if (day.isNotEmpty()) " ($day)" else ""}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // 🔥 IMPORTANTE: NO configurar sonido aquí en Android 8+
        // El sonido ya está configurado en el canal
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Solo para Android < 8, configurar sonido directamente
            if (reminder.sound && reminder.sound_uri != null) {
                try {
                    val soundUri = Uri.parse(reminder.sound_uri)
                    notificationBuilder.setSound(soundUri)
                    Log.d("ReminderReceiver", "   🔊 Sonido configurado (Android < 8): $soundUri")
                } catch (e: Exception) {
                    val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    notificationBuilder.setSound(defaultSound)
                    Log.e("ReminderReceiver", "   ⚠️ Error al cargar sonido: ${e.message}")
                }
            }
        } else {
            Log.d("ReminderReceiver", "   🔊 Sonido controlado por canal (Android 8+)")
        }

        // 🔥 Configurar vibración
        if (reminder.vibration) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            Log.d("ReminderReceiver", "   📳 Vibración: activada")
        } else {
            Log.d("ReminderReceiver", "   📳 Vibración: desactivada")
        }

        // 🔥 Luces
        notificationBuilder
            .setLights(Color.BLUE, 1000, 1000)
            .setDefaults(0) // Sin defaults automáticos

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = Random.nextInt(1000, 9999)
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d("ReminderReceiver", "   ✅ Notificación mostrada con ID: $notificationId")
    }
}