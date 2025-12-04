package com.example.app.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log

object NotificationHelper {
    const val CHANNEL_ID = "reminder_channel"
    private const val CHANNEL_NAME = "Recordatorios"
    private const val CHANNEL_DESCRIPTION = "Notificaciones de recordatorios"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 🔥 IMPORTANCE_HIGH es crítico para que suene
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                lightColor = Color.BLUE
                setShowBadge(true)
                // 🔥 Asegurar que suene
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                )
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "✅ Canal de notificaciones creado con IMPORTANCE_HIGH")
        }
    }

    /**
     * 🔥 NUEVO: Despertar el dispositivo cuando llega una notificación
     */
    fun wakeUpDevice(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "RecuerdaGo::NotificationWakeLock"
        )

        // Mantener despierto por 5 segundos
        wakeLock.acquire(5000)
        Log.d("NotificationHelper", "📱 Dispositivo despertado")
    }
}