package com.example.app.websocket

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.app.utils.SessionManager
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

    private var externalListener: WebSocketListener? = null
    private val broadcastListeners = mutableListOf<WebSocketListener>()
    private var currentToken: String? = null
    private val gson = Gson()

    // 🆕 Parámetros guardados para reconexión
    private var savedBaseUrl: String? = null
    private var sessionManager: SessionManager? = null
    private var isInitialized = false

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
                        return 
                    }
                    "pong" -> {
                        Log.v(TAG, "🏓 Pong recibido")
                        return
                    }
                }
            } catch (e: Exception) {
                Log.v(TAG, "ℹ️ Mensaje no es JSON, propagando...")
            }

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
            
            // Intentar reconectar si se cerró inesperadamente (no por logout)
            if (code != 1000 && savedBaseUrl != null && currentToken != null) {
                scheduleReconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "❌ Error en WebSocket: ${t.message}")
            externalListener?.onFailure(webSocket, t, response)
            broadcastListeners.forEach { it.onFailure(webSocket, t, response) }
            
            // 🆕 LÓGICA DE REINTENTO UNIFICADA
            if (savedBaseUrl != null && currentToken != null) {
                val delay = if (response?.code == 403) 30000L else 10000L
                Log.d(TAG, "🔄 Reintentando conexión en ${delay/1000}s...")
                scheduleReconnect(delay)
            }
        }
    }

    private fun scheduleReconnect(delay: Long = 10000L) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isConnected() && savedBaseUrl != null && currentToken != null) {
                Log.d(TAG, "🔄 Ejecutando reconexión programada...")
                connectGlobal(savedBaseUrl!!, currentToken!!)
            }
        }, delay)
    }

    /**
     * 🆕 Inicializar para observar cambios de token
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        sessionManager = SessionManager.getInstance(context)
        sessionManager?.addTokenChangeListener { newToken ->
            Log.d(TAG, "🔄 Token actualizado por SessionManager")
            currentToken = newToken
            if (isConnected()) {
                updateToken(newToken)
            } else if (savedBaseUrl != null) {
                // Si estaba desconectado, intentar abrir con el nuevo token
                Log.d(TAG, "🔄 WebSocket caído, intentando abrir con nuevo token...")
                connectGlobal(savedBaseUrl!!, newToken)
            }
        }
        isInitialized = true
        Log.d(TAG, "✅ WebSocketManager inicializado y vinculado a SessionManager")
    }

    /**
     * 🆕 Forzar verificación de salud (usar en onResume)
     */
    fun checkHealth() {
        if (!isConnected() && savedBaseUrl != null && currentToken != null) {
            Log.d(TAG, "🏥 Salud: WebSocket caído detectado en onResume, reconectando...")
            connectGlobal(savedBaseUrl!!, currentToken!!)
        } else {
            Log.v(TAG, "🏥 Salud: WebSocket OK")
        }
    }

    fun connectGlobal(baseUrl: String, token: String) {
        savedBaseUrl = baseUrl
        currentToken = token

        if (isConnected()) {
            Log.d(TAG, "⚠️ Ya está conectado, actualizando token...")
            updateToken(token)
            return
        }

        val wsUrl = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/ws/chat?token=$token"

        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "🔌 CONECTANDO GLOBALMENTE")
        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "   URL: ${wsUrl.substringBefore("?token=")}")

        connectInternal(wsUrl)
    }

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
        // Limpiar cliente anterior si existe
        client?.dispatcher?.executorService?.shutdown()
        
        client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client?.newWebSocket(request, internalListener)
    }

    fun updateToken(newToken: String) {
        if (!isConnected()) {
            Log.w(TAG, "⚠️ No conectado, no se puede actualizar token")
            return
        }

        Log.d(TAG, "🔄 ACTUALIZANDO TOKEN EN WEBSOCKET")
        currentToken = newToken
        val message = JSONObject().apply {
            put("type", "refresh_token")
            put("token", newToken)
        }.toString()

        send(message)
    }

    fun addBroadcastListener(listener: WebSocketListener) {
        if (!broadcastListeners.contains(listener)) {
            broadcastListeners.add(listener)
        }
    }

    fun removeBroadcastListener(listener: WebSocketListener) {
        broadcastListeners.remove(listener)
    }

    fun send(message: String): Boolean {
        return try {
            val sent = webSocket?.send(message) ?: false
            if (!sent) Log.w(TAG, "⚠️ No se pudo enviar mensaje (socket desconectado)")
            sent
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al enviar: ${e.message}")
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
        savedBaseUrl = null
        client?.dispatcher?.executorService?.shutdown()
        client = null
    }

    fun getCurrentToken(): String? = currentToken
}