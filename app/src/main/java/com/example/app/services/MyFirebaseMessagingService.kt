package com.example.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.app.MainActivity
import com.example.app.R
import com.example.app.network.RetrofitClient
import com.example.app.receivers.NotificationReplyReceiver
import com.example.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_Service"
        private const val CHANNEL_ID = "recuerdago_mensajes"
        private const val CHANNEL_NAME = "Mensajes de Grupos"
        private const val PREFS_NAME = "notification_messages"
        private const val MAX_MESSAGES_PER_GROUP = 10 // Máximo de mensajes a acumular
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "🔥 FirebaseMessagingService creado")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🆕 Nuevo token FCM generado:")
        Log.d(TAG, "   Token: ${token.take(30)}...")
        sendTokenToBackend(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "📨 ========================================")
        Log.d(TAG, "📨 MENSAJE FCM RECIBIDO")
        Log.d(TAG, "📨 ========================================")
        Log.d(TAG, "   From: ${message.from}")
        Log.d(TAG, "   Data: ${message.data}")

        // Obtener datos del mensaje
        val type = message.data["type"] ?: "nuevo_mensaje"
        val titulo = message.data["titulo"] ?: message.notification?.title ?: "Nuevo mensaje"
        val cuerpo = message.data["cuerpo"] ?: message.notification?.body ?: ""
        val grupoId = message.data["grupo_id"]?.toIntOrNull()
        val grupoNombre = message.data["grupo_nombre"]
        val remitenteNombre = message.data["remitente_nombre"]
        val timestamp = message.data["timestamp"]

        when (type) {
            "nuevo_mensaje" -> {
                // Acumular mensaje
                if (grupoId != null && remitenteNombre != null) {
                    addMessageToHistory(grupoId, remitenteNombre, cuerpo, timestamp)
                }

                showMessagingStyleNotification(
                    titulo = titulo,
                    grupoId = grupoId,
                    grupoNombre = grupoNombre
                )
            }
            else -> {
                showSimpleNotification(titulo, cuerpo, grupoId, grupoNombre, remitenteNombre)
            }
        }
    }

    /**
     * 💾 Guardar mensaje en historial local (para acumulación)
     */
    private fun addMessageToHistory(
        grupoId: Int,
        remitenteNombre: String,
        mensaje: String,
        timestamp: String?
    ) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "grupo_$grupoId"

        // Obtener mensajes existentes
        val existingMessages = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        // Crear nuevo mensaje con formato: timestamp|remitente|mensaje
        val time = timestamp ?: System.currentTimeMillis().toString()
        val newMessage = "$time|$remitenteNombre|$mensaje"

        // Agregar nuevo mensaje
        existingMessages.add(newMessage)

        // Mantener solo los últimos MAX_MESSAGES_PER_GROUP mensajes
        val messagesList = existingMessages.toList().takeLast(MAX_MESSAGES_PER_GROUP)

        // Guardar
        prefs.edit().putStringSet(key, messagesList.toSet()).apply()

        Log.d(TAG, "💾 Mensaje guardado en historial. Total: ${messagesList.size}")
    }

    /**
     * 📖 Obtener historial de mensajes del grupo
     */
    private fun getMessageHistory(grupoId: Int): List<Triple<Long, String, String>> {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "grupo_$grupoId"
        val messages = prefs.getStringSet(key, emptySet()) ?: emptySet()

        return messages.mapNotNull { messageStr ->
            val parts = messageStr.split("|")
            if (parts.size == 3) {
                val timestamp = parts[0].toLongOrNull() ?: System.currentTimeMillis()
                val remitente = parts[1]
                val mensaje = parts[2]
                Triple(timestamp, remitente, mensaje)
            } else null
        }.sortedBy { it.first } // Ordenar por timestamp
    }

    /**
     * 🧹 Limpiar historial de mensajes del grupo
     */
    private fun clearMessageHistory(grupoId: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("grupo_$grupoId").apply()
        Log.d(TAG, "🧹 Historial de mensajes limpiado para grupo $grupoId")
    }

    /**
     * 🎨 Notificación estilo mensajería con acumulación y respuesta directa
     */
    private fun showMessagingStyleNotification(
        titulo: String,
        grupoId: Int?,
        grupoNombre: String?
    ) {
        if (grupoId == null) return

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent para abrir el chat
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("GRUPO_ID", grupoId)
            grupoNombre?.let { putExtra("GRUPO_NOMBRE", it) }
        }

        val openPendingIntent = PendingIntent.getActivity(
            this,
            grupoId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crear Person para "yo" (el usuario actual)
        val me = Person.Builder()
            .setName("Tú")
            .build()

        // Obtener historial de mensajes
        val messageHistory = getMessageHistory(grupoId)

        Log.d(TAG, "📖 Mostrando ${messageHistory.size} mensajes acumulados")

        // Estilo de mensajería con historial
        val messagingStyle = NotificationCompat.MessagingStyle(me)
            .setConversationTitle(titulo)

        // Agregar todos los mensajes del historial
        messageHistory.forEach { (timestamp, remitente, mensaje) ->
            val sender = Person.Builder()
                .setName(remitente)
                .build()

            messagingStyle.addMessage(mensaje, timestamp, sender)
        }

        // ✍️ RESPUESTA DIRECTA: Crear RemoteInput
        val remoteInput = RemoteInput.Builder(NotificationReplyReceiver.KEY_TEXT_REPLY)
            .setLabel("Responder...")
            .build()

        // Intent para enviar la respuesta
        val replyIntent = Intent(this, NotificationReplyReceiver::class.java).apply {
            putExtra(NotificationReplyReceiver.EXTRA_GRUPO_ID, grupoId)
            putExtra(NotificationReplyReceiver.EXTRA_NOTIFICATION_ID, grupoId)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            this,
            grupoId + 1000, // ID único diferente
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE // ⚠️ MUTABLE para RemoteInput
        )

        // Acción de respuesta directa
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            "Responder",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true) // Respuestas inteligentes
            .build()

        // Construir notificación
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(replyAction) // ✍️ Agregar acción de respuesta
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setLights(0xFF4CAF50.toInt(), 1000, 500)
            .setColor(0xFF4CAF50.toInt())
            .setGroup("grupo_$grupoId")
            .setGroupSummary(true) // Importante para acumulación
            .setNumber(messageHistory.size) // Badge con cantidad de mensajes
            .build()

        notificationManager.notify(grupoId, notification)
        Log.d(TAG, "🔔 Notificación con ${messageHistory.size} mensajes y respuesta directa mostrada")
    }

    /**
     * 🔔 Notificación simple (fallback)
     */
    private fun showSimpleNotification(
        titulo: String,
        cuerpo: String,
        grupoId: Int?,
        grupoNombre: String?,
        remitenteNombre: String?
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            grupoId?.let { putExtra("GRUPO_ID", it) }
            grupoNombre?.let { putExtra("GRUPO_NOMBRE", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            grupoId ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setColor(0xFF4CAF50.toInt())
            .build()

        notificationManager.notify(grupoId ?: 0, notification)
        Log.d(TAG, "🔔 Notificación simple mostrada")
    }

    private fun sendTokenToBackend(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionManager = SessionManager.getInstance(applicationContext)
                val accessToken = sessionManager.getAccessToken()

                if (accessToken.isNullOrEmpty()) {
                    Log.w(TAG, "⚠️ No hay token de acceso, guardando token FCM localmente")
                    saveFCMTokenLocally(token)
                    return@launch
                }

                Log.d(TAG, "📤 Enviando token FCM al backend...")

                val request = mapOf(
                    "token" to token,
                    "dispositivo" to "android"
                )

                val response = RetrofitClient.apiService.registrarFCMToken(
                    "Bearer $accessToken",
                    request
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Token FCM registrado en backend correctamente")
                    clearLocalFCMToken()
                } else {
                    Log.e(TAG, "❌ Error al registrar token: ${response.code()}")
                    saveFCMTokenLocally(token)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción al enviar token: ${e.message}")
                e.printStackTrace()
                saveFCMTokenLocally(token)
            }
        }
    }

    private fun saveFCMTokenLocally(token: String) {
        val prefs = getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("PENDING_FCM_TOKEN", token).apply()
        Log.d(TAG, "💾 Token FCM guardado localmente para envío posterior")
    }

    private fun clearLocalFCMToken() {
        val prefs = getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("PENDING_FCM_TOKEN").apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de mensajes en grupos"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                enableLights(true)
                lightColor = 0xFF4CAF50.toInt()
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "📢 Canal de notificaciones creado")
        }
    }
}