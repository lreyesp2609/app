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

    fun createAlertChannel(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "zona_peligro_alerts"

            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // Eliminar canal existente para recrearlo
            notificationManager.deleteNotificationChannel(channelId)

            // 🔥 SISTEMA DE FALLBACK: Probar múltiples fuentes de sonido
            val soundUri = obtenerSonidoAlerta(context)

            val channel = NotificationChannel(
                channelId,
                "Alertas de Zona Peligrosa",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas críticas cuando entras a zonas peligrosas"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC

                // Usar el sonido obtenido con fallback
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )

                // Bypass Do Not Disturb (si el usuario da permiso)
                try {
                    setBypassDnd(true)
                } catch (e: Exception) {
                    Log.w("NotificationHelper", "⚠️ No se pudo activar bypass DND: ${e.message}")
                }
            }

            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "✅ Canal de alertas creado con sonido: $soundUri")

            return channelId
        }
        return "default"
    }

    /**
     * 🔥 Obtiene URI de sonido con sistema de fallback
     * Prioridad:
     * 1. TYPE_ALARM (más fuerte, para emergencias)
     * 2. TYPE_RINGTONE (medio, tono de llamada)
     * 3. TYPE_NOTIFICATION (suave, última opción)
     */
    private fun obtenerSonidoAlerta(context: Context): Uri {
        // 1️⃣ Intentar TYPE_ALARM
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alarmUri != null && esSonidoValido(context, alarmUri)) {
            Log.d("NotificationHelper", "✅ Usando TYPE_ALARM")
            return alarmUri
        }

        Log.w("NotificationHelper", "⚠️ TYPE_ALARM no disponible, usando fallback...")

        // 2️⃣ Fallback: TYPE_RINGTONE
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (ringtoneUri != null && esSonidoValido(context, ringtoneUri)) {
            Log.d("NotificationHelper", "✅ Usando TYPE_RINGTONE como fallback")
            return ringtoneUri
        }

        // 3️⃣ Último recurso: TYPE_NOTIFICATION
        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (notificationUri != null && esSonidoValido(context, notificationUri)) {
            Log.d("NotificationHelper", "✅ Usando TYPE_NOTIFICATION como último recurso")
            return notificationUri
        }

        // 4️⃣ Si TODO falla, usar sonido del sistema
        Log.e("NotificationHelper", "❌ No hay sonidos disponibles, usando Settings.System.DEFAULT_NOTIFICATION_URI")
        return android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
    }

    /**
     * Verifica si un URI de sonido es válido y reproducible
     */
    private fun esSonidoValido(context: Context, uri: Uri): Boolean {
        return try {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            val valido = ringtone != null
            ringtone?.stop() // Detener si se reprodujo
            valido
        } catch (e: Exception) {
            Log.w("NotificationHelper", "⚠️ URI inválido: $uri - ${e.message}")
            false
        }
    }

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