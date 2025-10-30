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
import com.google.firebase.messaging.FirebaseMessaging
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
        private const val MAX_MESSAGES_PER_GROUP = 10
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "🔥 FirebaseMessagingService creado")

        // ✅ NUEVO: Verificar y enviar token al crear el servicio
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "📱 Token FCM actual verificado:")
                Log.d(TAG, "   Token: ${token?.take(30)}...")
                Log.d(TAG, "   Token completo (para debugging): $token")
                token?.let { sendTokenToBackend(it) }
            } else {
                Log.e(TAG, "❌ Error obteniendo token FCM: ${task.exception}")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🆕 ========================================")
        Log.d(TAG, "🆕 NUEVO TOKEN FCM GENERADO")
        Log.d(TAG, "🆕 ========================================")
        Log.d(TAG, "   Token preview: ${token.take(30)}...")
        Log.d(TAG, "   Token completo: $token")
        sendTokenToBackend(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "📨 ========================================")
        Log.d(TAG, "📨 MENSAJE FCM RECIBIDO")
        Log.d(TAG, "📨 ========================================")
        Log.d(TAG, "   From: ${message.from}")
        Log.d(TAG, "   Notification: ${message.notification}")
        Log.d(TAG, "   Data: ${message.data}")

        val type = message.data["type"] ?: "nuevo_mensaje"
        val titulo = message.data["titulo"] ?: message.notification?.title ?: "Nuevo mensaje"
        val cuerpo = message.data["cuerpo"] ?: message.notification?.body ?: ""
        val grupoId = message.data["grupo_id"]?.toIntOrNull()
        val grupoNombre = message.data["grupo_nombre"]
        val remitenteNombre = message.data["remitente_nombre"]
        val timestamp = message.data["timestamp"]

        Log.d(TAG, "   Type: $type")
        Log.d(TAG, "   GrupoId: $grupoId")
        Log.d(TAG, "   Remitente: $remitenteNombre")

        when (type) {
            "nuevo_mensaje" -> {
                if (grupoId != null && remitenteNombre != null) {
                    Log.d(TAG, "📝 Procesando mensaje del grupo $grupoId")
                    addMessageToHistory(grupoId, remitenteNombre, cuerpo, timestamp)
                    showMessagingStyleNotification(titulo, grupoId, grupoNombre)
                } else {
                    Log.w(TAG, "⚠️ Datos incompletos: grupoId=$grupoId, remitente=$remitenteNombre")
                }
            }
            else -> {
                showSimpleNotification(titulo, cuerpo, grupoId, grupoNombre, remitenteNombre)
            }
        }
    }

    private fun addMessageToHistory(
        grupoId: Int,
        remitenteNombre: String,
        mensaje: String,
        timestamp: String?
    ) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "grupo_$grupoId"

        val existingMessages = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val time = timestamp ?: System.currentTimeMillis().toString()
        val newMessage = "$time|$remitenteNombre|$mensaje"

        existingMessages.add(newMessage)
        val messagesList = existingMessages.toList().takeLast(MAX_MESSAGES_PER_GROUP)

        prefs.edit().putStringSet(key, messagesList.toSet()).apply()
        Log.d(TAG, "💾 Mensaje guardado en historial. Total: ${messagesList.size}")
    }

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
        }.sortedBy { it.first }
    }

    private fun clearMessageHistory(grupoId: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("grupo_$grupoId").apply()
        Log.d(TAG, "🧹 Historial limpiado para grupo $grupoId")
    }

    private fun showMessagingStyleNotification(
        titulo: String,
        grupoId: Int,
        grupoNombre: String?
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 🧹 CRÍTICO: Cancelar cualquier notificación previa del grupo
        notificationManager.cancel("grupo_chat_$grupoId", grupoId)

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

        val me = Person.Builder()
            .setName("Tú")
            .setKey("current_user")
            .build()

        val messageHistory = getMessageHistory(grupoId)
        Log.d(TAG, "📖 Mostrando ${messageHistory.size} mensajes acumulados")

        val messagingStyle = NotificationCompat.MessagingStyle(me)
            .setConversationTitle(titulo)
            .setGroupConversation(true)

        messageHistory.forEach { (timestamp, remitente, mensaje) ->
            val sender = Person.Builder()
                .setName(remitente)
                .setKey("user_${remitente.hashCode()}")
                .build()

            messagingStyle.addMessage(mensaje, timestamp, sender)
        }

        val remoteInput = RemoteInput.Builder(NotificationReplyReceiver.KEY_TEXT_REPLY)
            .setLabel("Responder...")
            .build()

        val replyIntent = Intent(this, NotificationReplyReceiver::class.java).apply {
            putExtra(NotificationReplyReceiver.EXTRA_GRUPO_ID, grupoId)
            putExtra(NotificationReplyReceiver.EXTRA_NOTIFICATION_ID, grupoId)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            this,
            grupoId + 1000,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            "Responder",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(replyAction)
            .setOnlyAlertOnce(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setLights(0xFF4CAF50.toInt(), 1000, 500)
            .setColor(0xFF4CAF50.toInt())
            .setNumber(messageHistory.size)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()

        val notificationTag = "grupo_chat_$grupoId"
        notificationManager.notify(notificationTag, grupoId, notification)
        Log.d(TAG, "🔔 Notificación actualizada con ${messageHistory.size} mensajes (tag: $notificationTag, id: $grupoId)")
    }

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
                    Log.w(TAG, "⚠️ ========================================")
                    Log.w(TAG, "⚠️ NO HAY TOKEN DE ACCESO")
                    Log.w(TAG, "⚠️ Guardando FCM localmente para envío posterior")
                    Log.w(TAG, "⚠️ ========================================")
                    saveFCMTokenLocally(token)
                    return@launch
                }

                Log.d(TAG, "📤 ========================================")
                Log.d(TAG, "📤 ENVIANDO TOKEN FCM AL BACKEND")
                Log.d(TAG, "📤 ========================================")
                Log.d(TAG, "   Token FCM: ${token.take(30)}...")
                Log.d(TAG, "   Access Token: ${accessToken.take(20)}...")

                val request = mapOf(
                    "token" to token,
                    "dispositivo" to "android"
                )

                val response = RetrofitClient.apiService.registrarFCMToken(
                    "Bearer $accessToken",
                    request
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ ========================================")
                    Log.d(TAG, "✅ TOKEN FCM REGISTRADO EN BACKEND")
                    Log.d(TAG, "✅ HTTP ${response.code()}")
                    Log.d(TAG, "✅ ========================================")
                    clearLocalFCMToken()
                } else {
                    Log.e(TAG, "❌ ========================================")
                    Log.e(TAG, "❌ ERROR AL REGISTRAR TOKEN")
                    Log.e(TAG, "❌ HTTP ${response.code()}")
                    Log.e(TAG, "❌ ${response.errorBody()?.string()}")
                    Log.e(TAG, "❌ ========================================")
                    saveFCMTokenLocally(token)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ ========================================")
                Log.e(TAG, "❌ EXCEPCIÓN AL ENVIAR TOKEN")
                Log.e(TAG, "❌ ${e.message}")
                Log.e(TAG, "❌ ========================================")
                e.printStackTrace()
                saveFCMTokenLocally(token)
            }
        }
    }

    private fun saveFCMTokenLocally(token: String) {
        val prefs = getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("PENDING_FCM_TOKEN", token).apply()
        Log.d(TAG, "💾 ========================================")
        Log.d(TAG, "💾 TOKEN GUARDADO LOCALMENTE")
        Log.d(TAG, "💾 Se enviará después del login")
        Log.d(TAG, "💾 Token: ${token.take(30)}...")
        Log.d(TAG, "💾 ========================================")
    }

    private fun clearLocalFCMToken() {
        val prefs = getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("PENDING_FCM_TOKEN").apply()
        Log.d(TAG, "🧹 Token local eliminado (ya registrado en backend)")
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