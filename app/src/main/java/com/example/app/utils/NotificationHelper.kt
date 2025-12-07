package com.example.app.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log

object NotificationHelper {
    const val CHANNEL_ID = "reminder_channel"
    private const val CHANNEL_NAME = "Recordatorios"
    private const val CHANNEL_DESCRIPTION = "Notificaciones de recordatorios"

    /**
     * Crea el canal de notificaciones por defecto (para recordatorios sin sonido personalizado)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                lightColor = Color.BLUE
                setShowBadge(true)
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
            Log.d("NotificationHelper", "✅ Canal de notificaciones por defecto creado")
        }
    }

    /**
     * 🆕 Crea un canal de notificación con sonido personalizado
     * Retorna el ID del canal creado
     */
    fun createCustomSoundChannel(context: Context, soundUri: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Crear un ID único basado en la URI del sonido
            val channelId = "reminder_${soundUri.hashCode()}"

            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // Si el canal ya existe, retornarlo
            if (notificationManager.getNotificationChannel(channelId) != null) {
                Log.d("NotificationHelper", "✅ Canal existente reutilizado: $channelId")
                return channelId
            }

            try {
                val parsedUri = Uri.parse(soundUri)

                // Otorgar permisos persistentes si es necesario
                try {
                    context.contentResolver.takePersistableUriPermission(
                        parsedUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    Log.w("NotificationHelper", "No se pudo otorgar permiso persistente: ${e.message}")
                }

                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(channelId, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                    lightColor = Color.BLUE
                    setShowBadge(true)

                    // 🔥 CONFIGURAR SONIDO PERSONALIZADO
                    setSound(
                        parsedUri,
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .build()
                    )

                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                notificationManager.createNotificationChannel(channel)
                Log.d("NotificationHelper", "✅ Canal con sonido personalizado creado: $channelId")
                return channelId

            } catch (e: Exception) {
                Log.e("NotificationHelper", "❌ Error al crear canal personalizado: ${e.message}")
                // Si falla, retornar el canal por defecto
                return CHANNEL_ID
            }
        }

        // Para Android < 8, retornar el canal por defecto
        return CHANNEL_ID
    }

    /**
     * 🆕 Crea un canal sin sonido (para notificaciones solo con vibración)
     */
    fun createSilentChannel(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "reminder_silent"

            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // Si el canal ya existe, retornarlo
            if (notificationManager.getNotificationChannel(channelId) != null) {
                return channelId
            }

            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, "$CHANNEL_NAME (Silencioso)", importance).apply {
                description = "Recordatorios sin sonido"
                enableVibration(true)
                enableLights(true)
                lightColor = Color.BLUE
                setShowBadge(true)
                setSound(null, null) // Sin sonido
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "✅ Canal silencioso creado")
            return channelId
        }

        return CHANNEL_ID
    }

    /**
     * Despertar el dispositivo cuando llega una notificación
     */
    fun wakeUpDevice(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "RecuerdaGo::NotificationWakeLock"
        )

        wakeLock.acquire(5000)
        Log.d("NotificationHelper", "📱 Dispositivo despertado")
    }
}