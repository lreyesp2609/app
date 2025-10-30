package com.example.app.websocket

import android.content.Context
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
import kotlin.collections.get

/**
 * Manager para WebSocket de Ubicaciones
 * Soporta conexión global (AppNavigation) y listeners por pantalla
 * 🆕 Actualiza automáticamente el token cuando SessionManager lo refresca
 */
object WebSocketLocationManager {
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var externalListener: WebSocketListener? = null
    private val broadcastListeners = mutableListOf<WebSocketListener>()
    private var currentToken: String? = null
    private val gson = Gson()
    private const val TAG = "WS_LocationManager"

    // 🆕 Referencia al listener de token
    private var tokenChangeListener: ((String) -> Unit)? = null
    private var sessionManager: SessionManager? = null

    private val internalListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "✅ ════════════════════════════════════════")
            Log.d(TAG, "✅ WEBSOCKET DE UBICACIONES CONECTADO")
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
     * 🆕 Inicializar y registrar listener de tokens
     * DEBE llamarse UNA SOLA VEZ al inicio
     */
    @Synchronized
    fun initialize(context: Context) {
        // Si ya hay listener registrado, no hacer nada
        if (tokenChangeListener != null) {
            Log.d(TAG, "⚠️ Ya está inicializado, ignorando llamada duplicada")
            return
        }

        sessionManager = SessionManager.getInstance(context.applicationContext)

        // 🆕 Crear y registrar listener de cambios de token
        tokenChangeListener = { newToken ->
            Log.d(TAG, "🔄 ════════════════════════════════════════")
            Log.d(TAG, "🔄 TOKEN ACTUALIZADO POR SESSIONMANAGER")
            Log.d(TAG, "🔄 ════════════════════════════════════════")
            Log.d(TAG, "   Nuevo token: ${newToken.take(20)}...")
            Log.d(TAG, "   WebSocket conectado: ${isConnected()}")

            if (isConnected()) {
                updateToken(newToken)
            } else {
                Log.w(TAG, "⚠️ WebSocket no conectado, guardando token para siguiente conexión")
                currentToken = newToken
            }
        }

        sessionManager?.addTokenChangeListener(tokenChangeListener!!)

        Log.d(TAG, "✅ ════════════════════════════════════════")
        Log.d(TAG, "✅ WEBSOCKET MANAGER INICIALIZADO")
        Log.d(TAG, "✅ Listener de tokens registrado")
        Log.d(TAG, "✅ Total listeners en SessionManager: ${sessionManager?.getListenerCount()}")
        Log.d(TAG, "✅ ════════════════════════════════════════")
    }

    /**
     * 🆕 Conectar desde AppNavigation o LocationService
     */
    fun connectGlobal(baseUrl: String, token: String) {
        if (isConnected()) {
            Log.d(TAG, "⚠️ Ya está conectado, actualizando token...")
            updateToken(token)
            return
        }

        val wsUrl = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/grupos/ws/ubicaciones?token=$token"

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

    fun addBroadcastListener(listener: WebSocketListener) {
        if (!broadcastListeners.contains(listener)) {
            broadcastListeners.add(listener)
            Log.d(TAG, "📢 Listener agregado. Total: ${broadcastListeners.size}")
        }
    }

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
        Log.d(TAG, "🔒 ════════════════════════════════════════")
        Log.d(TAG, "🔒 CERRANDO WEBSOCKET DE UBICACIONES")
        Log.d(TAG, "🔒 ════════════════════════════════════════")

        // 🆕 Desregistrar listener de tokens
        tokenChangeListener?.let { listener ->
            sessionManager?.removeTokenChangeListener(listener)
            Log.d(TAG, "➖ Listener de tokens desregistrado")
        }
        tokenChangeListener = null
        sessionManager = null

        webSocket?.close(1000, "Cliente cerró la conexión")
        webSocket = null
        externalListener = null
        broadcastListeners.clear()
        currentToken = null
        client?.dispatcher?.executorService?.shutdown()
        client = null

        Log.d(TAG, "✅ WebSocket cerrado y recursos liberados")
    }

    fun getCurrentToken(): String? = currentToken
}