package com.example.app.websocket

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Singleton para gestionar WebSocket de notificaciones globales
 * Mantiene actualizado el conteo de mensajes no leídos por grupo
 */
object NotificationWebSocketManager {
    private const val TAG = "WS_Notifications"

    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private val gson = Gson()

    // Estado de mensajes no leídos por grupo (grupoId -> count)
    private val _unreadCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<Int, Int>> = _unreadCounts

    // Estado de conexión
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val internalListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "✅ ════════════════════════════════════════")
            Log.d(TAG, "✅ WEBSOCKET DE NOTIFICACIONES CONECTADO")
            Log.d(TAG, "✅ ════════════════════════════════════════")
            _isConnected.value = true
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                Log.v(TAG, "📨 Mensaje recibido: ${text.take(100)}")
                val json = JSONObject(text)
                val type = json.optString("type")

                when (type) {
                    "unread_count_update" -> {
                        handleUnreadCountUpdate(json)
                    }
                    "pong" -> {
                        Log.v(TAG, "🏓 Pong recibido")
                    }
                    "token_refreshed" -> {
                        val message = json.optString("message")
                        Log.d(TAG, "🔄 Token actualizado en servidor: $message")
                    }
                    "error" -> {
                        val message = json.optString("message")
                        val code = json.optString("code")
                        Log.e(TAG, "❌ Error del servidor [$code]: $message")

                        if (code == "TOKEN_EXPIRED") {
                            Log.e(TAG, "🔒 Token expirado, cerrando WebSocket")
                            close()
                        }
                    }
                    else -> {
                        Log.w(TAG, "⚠️ Tipo de mensaje no manejado: $type")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error procesando mensaje: ${e.message}")
                e.printStackTrace()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "❌ Error en WebSocket: ${t.message}")
            _isConnected.value = false
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "⚠️ WebSocket cerrándose: $code - $reason")
            _isConnected.value = false
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "🔒 WebSocket cerrado: $code - $reason")
            _isConnected.value = false
        }
    }

    /**
     * Conecta al WebSocket de notificaciones
     */
    fun connect(baseUrl: String, token: String) {
        if (isConnected()) {
            Log.d(TAG, "⚠️ Ya está conectado, actualizando token...")
            updateToken(token)
            return
        }

        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "🔌 CONECTANDO WEBSOCKET DE NOTIFICACIONES")
        Log.d(TAG, "🔌 ════════════════════════════════════════")

        val wsUrl = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/grupos/ws/notificaciones?token=$token"

        Log.d(TAG, "   URL: ${wsUrl.substringBefore("?token=")}")

        client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client?.newWebSocket(request, internalListener)
    }

    /**
     * 🆕 Actualizar token sin reconectar
     */
    fun updateToken(newToken: String) {
        if (!isConnected()) {
            Log.w(TAG, "⚠️ No conectado, no se puede actualizar token")
            return
        }

        Log.d(TAG, "🔄 ════════════════════════════════════════")
        Log.d(TAG, "🔄 ACTUALIZANDO TOKEN EN WEBSOCKET")
        Log.d(TAG, "🔄 ════════════════════════════════════════")

        val message = JSONObject().apply {
            put("type", "refresh_token")
            put("token", newToken)
        }.toString()

        val sent = send(message)
        if (sent) {
            Log.d(TAG, "✅ Token enviado al servidor")
        } else {
            Log.e(TAG, "❌ Error al enviar token")
        }
    }

    /**
     * Procesa actualizaciones de mensajes no leídos
     */
    private fun handleUnreadCountUpdate(json: JSONObject) {
        try {
            val data = json.getJSONArray("data")
            val newCounts = mutableMapOf<Int, Int>()

            Log.d(TAG, "📊 ════════════════════════════════════════")
            Log.d(TAG, "📊 ACTUALIZACIÓN DE MENSAJES NO LEÍDOS")
            Log.d(TAG, "📊 ════════════════════════════════════════")

            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val grupoId = item.getInt("grupo_id")
                val count = item.getInt("mensajes_no_leidos")
                newCounts[grupoId] = count
                Log.d(TAG, "   Grupo $grupoId: $count mensajes")
            }

            _unreadCounts.value = newCounts
            Log.d(TAG, "✅ Conteos actualizados correctamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error procesando conteos: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Envía un mensaje al servidor
     */
    fun send(message: String): Boolean {
        return try {
            val sent = webSocket?.send(message) ?: false
            if (sent) {
                Log.v(TAG, "📤 Mensaje enviado: ${message.take(50)}...")
            } else {
                Log.w(TAG, "⚠️ No se pudo enviar mensaje (WebSocket no conectado)")
            }
            sent
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al enviar mensaje: ${e.message}")
            false
        }
    }

    /**
     * Verifica si está conectado
     */
    fun isConnected(): Boolean {
        return webSocket != null && _isConnected.value
    }

    /**
     * Cierra la conexión
     */
    fun close() {
        Log.d(TAG, "🔒 ════════════════════════════════════════")
        Log.d(TAG, "🔒 CERRANDO WEBSOCKET DE NOTIFICACIONES")
        Log.d(TAG, "🔒 ════════════════════════════════════════")

        webSocket?.close(1000, "Cliente cerró la conexión")
        webSocket = null
        _isConnected.value = false
        _unreadCounts.value = emptyMap()

        client?.dispatcher?.executorService?.shutdown()
        client = null

        Log.d(TAG, "✅ WebSocket cerrado y recursos liberados")
    }

    /**
     * Obtiene el conteo de no leídos para un grupo específico
     */
    fun getUnreadCount(grupoId: Int): Int {
        return _unreadCounts.value[grupoId] ?: 0
    }
}