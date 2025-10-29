package com.example.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.app.utils.SessionManager
import com.example.app.websocket.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class NotificationReplyReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReply"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_GRUPO_ID = "grupo_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📨 Respuesta recibida desde notificación")

        // Obtener el texto de respuesta
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()

        if (replyText.isNullOrBlank()) {
            Log.w(TAG, "⚠️ Texto de respuesta vacío")
            return
        }

        // Obtener datos del grupo
        val grupoId = intent.getIntExtra(EXTRA_GRUPO_ID, -1)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (grupoId == -1) {
            Log.e(TAG, "❌ ID de grupo inválido")
            return
        }

        Log.d(TAG, "📤 Enviando respuesta:")
        Log.d(TAG, "   Grupo ID: $grupoId")
        Log.d(TAG, "   Mensaje: $replyText")

        // Enviar mensaje via WebSocket
        sendMessageViaWebSocket(context, grupoId, replyText, notificationId)
    }

    private fun sendMessageViaWebSocket(
        context: Context,
        grupoId: Int,
        mensaje: String,
        notificationId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionManager = SessionManager.getInstance(context)
                val accessToken = sessionManager.getAccessToken()

                if (accessToken.isNullOrEmpty()) {
                    Log.e(TAG, "❌ No hay token de acceso")
                    showErrorNotification(context, notificationId, "Error: Sesión expirada")
                    return@launch
                }

                // Verificar si el WebSocket está conectado
                if (!WebSocketManager.isConnected()) {
                    Log.w(TAG, "⚠️ WebSocket no conectado, conectando...")

                    // Conectar WebSocket si no está conectado
                    val baseUrl = com.example.app.BuildConfig.BASE_URL
                    WebSocketManager.connectGlobal(baseUrl, accessToken)

                    // Esperar un momento para que se establezca la conexión
                    delay(1000)

                    if (!WebSocketManager.isConnected()) {
                        Log.e(TAG, "❌ No se pudo conectar al WebSocket")
                        showErrorNotification(context, notificationId, "Error de conexión")
                        return@launch
                    }
                }

                Log.d(TAG, "📡 Enviando mensaje al WebSocket...")

                // Crear JSON del mensaje
                val messageJson = JSONObject().apply {
                    put("action", "mensaje")
                    put("grupo_id", grupoId)
                    put("contenido", mensaje)
                    put("tipo", "texto")
                }.toString()

                // Enviar via WebSocket
                val sent = WebSocketManager.send(messageJson)

                if (sent) {
                    Log.d(TAG, "✅ Mensaje enviado correctamente via WebSocket")

                    // Cancelar la notificación después de responder
                    delay(500) // Pequeño delay para que se vea el envío
                    NotificationManagerCompat.from(context).cancel(notificationId)

                    Log.d(TAG, "🔕 Notificación cancelada")
                } else {
                    Log.e(TAG, "❌ Error al enviar mensaje via WebSocket")
                    showErrorNotification(context, notificationId, "Error al enviar")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción al enviar mensaje: ${e.message}")
                e.printStackTrace()
                showErrorNotification(context, notificationId, "Error de conexión")
            }
        }
    }

    private fun showErrorNotification(context: Context, notificationId: Int, message: String) {
        Log.e(TAG, "❌ $message")
        // Opcionalmente podrías mostrar una notificación de error aquí
    }
}