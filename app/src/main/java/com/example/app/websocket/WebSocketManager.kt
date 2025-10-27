package com.example.app.websocket

import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manager para WebSocket de Chat
 * Soporta conexión global (AppNavigation) y listeners por pantalla
 */
object WebSocketManager {
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private const val TAG = "WS_ChatManager"

    // 🆕 Listener externo (para ChatGrupoScreen)
    private var externalListener: WebSocketListener? = null

    // 🆕 Lista de listeners para broadcast
    private val broadcastListeners = mutableListOf<WebSocketListener>()

    private var currentToken: String? = null
    private val gson = Gson()

    // 🆕 Listener interno que hace BROADCAST
    private val internalListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "✅ ════════════════════════════════════════")
            Log.d(TAG, "✅ WEBSOCKET DE CHAT CONECTADO")
            Log.d(TAG, "✅ ════════════════════════════════════════")

            externalListener?.onOpen(webSocket, response)
            broadcastListeners.forEach { it.onOpen(webSocket, response) }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.v(TAG, "📨 Mensaje recibido: ${text.take(100)}")

            try {
                val json = JSONObject(text)
                val type = json.optString("type")

                // 🔄 Manejar mensajes de token
                when (type) {
                    "refresh_token", "token_refreshed" -> {
                        Log.d(TAG, "🔄 Mensaje de token detectado: $type")
                        if (type == "refresh_token") {
                            val newToken = json.optString("token")
                            if (newToken.isNotEmpty()) {
                                currentToken = newToken
                                Log.d(TAG, "✅ Token actualizado: ${newToken.take(20)}...")
                            }
                        }
                        return // No propagar mensajes internos
                    }
                    "pong" -> {
                        Log.v(TAG, "🏓 Pong recibido")
                        return
                    }
                }
            } catch (e: Exception) {
                Log.v(TAG, "ℹ️ Mensaje no es JSON, propagando...")
            }

            // 📤 Propagar a TODOS los listeners
            externalListener?.onMessage(webSocket, text)
            broadcastListeners.forEach {
                try {
                    it.onMessage(webSocket, text)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error en listener: ${e.message}")
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "⚠️ WebSocket cerrándose: $code - $reason")
            externalListener?.onClosing(webSocket, code, reason)
            broadcastListeners.forEach { it.onClosing(webSocket, code, reason) }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "🔒 WebSocket cerrado: $code - $reason")
            externalListener?.onClosed(webSocket, code, reason)
            broadcastListeners.forEach { it.onClosed(webSocket, code, reason) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "❌ Error: ${t.message}")
            externalListener?.onFailure(webSocket, t, response)
            broadcastListeners.forEach { it.onFailure(webSocket, t, response) }
        }
    }

    /**
     * 🆕 Conectar desde AppNavigation (sin listener externo)
     */
    fun connectGlobal(baseUrl: String, token: String) {
        if (isConnected()) {
            Log.d(TAG, "⚠️ Ya está conectado, actualizando token...")
            updateToken(token)
            return
        }

        val wsUrl = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/grupos/ws/chat?token=$token"

        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "🔌 CONECTANDO GLOBALMENTE")
        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "   URL: ${wsUrl.substringBefore("?token=")}")

        currentToken = token
        connectInternal(wsUrl)
    }

    /**
     * Conectar con listener externo (para ChatGrupoScreen)
     */
    fun connect(url: String, listener: WebSocketListener) {
        if (isConnected()) {
            Log.d(TAG, "✅ Ya conectado, registrando listener externo")
            externalListener = listener
            return
        }

        Log.d(TAG, "🔌 Conectando con listener externo...")
        externalListener = listener
        connectInternal(url)
    }

    private fun connectInternal(url: String) {
        client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
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

        currentToken = newToken
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
     * 🆕 Registrar listener adicional
     */
    fun addBroadcastListener(listener: WebSocketListener) {
        if (!broadcastListeners.contains(listener)) {
            broadcastListeners.add(listener)
            Log.d(TAG, "📢 Listener agregado. Total: ${broadcastListeners.size}")
        }
    }

    /**
     * 🆕 Desregistrar listener
     */
    fun removeBroadcastListener(listener: WebSocketListener) {
        val removed = broadcastListeners.remove(listener)
        if (removed) {
            Log.d(TAG, "📢 Listener removido. Total: ${broadcastListeners.size}")
        }
    }

    fun send(message: String): Boolean {
        return try {
            val sent = webSocket?.send(message) ?: false
            if (sent) {
                Log.v(TAG, "📤 Mensaje enviado")
            } else {
                Log.w(TAG, "⚠️ No conectado")
            }
            sent
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            false
        }
    }

    fun isConnected(): Boolean {
        return webSocket != null
    }

    fun close() {
        Log.d(TAG, "🔒 Cerrando WebSocket")
        webSocket?.close(1000, "Cliente cerró la conexión")
        webSocket = null
        externalListener = null
        broadcastListeners.clear()
        currentToken = null
        client?.dispatcher?.executorService?.shutdown()
        client = null
    }

    fun getCurrentToken(): String? = currentToken
}