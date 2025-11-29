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

object WebSocketLocationManager {
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var externalListener: WebSocketListener? = null
    private val broadcastListeners = mutableListOf<WebSocketListener>()
    private var currentToken: String? = null
    private val gson = Gson()
    private const val TAG = "WS_LocationManager"

    // 🆕 Estado real de la conexión
    @Volatile
    private var isWebSocketConnected = false

    // Referencia al listener de token
    private var tokenChangeListener: ((String) -> Unit)? = null
    private var sessionManager: SessionManager? = null

    private val internalListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            // ✅ MARCAR COMO CONECTADO
            isWebSocketConnected = true

            Log.d(TAG, "✅ ════════════════════════════════════════")
            Log.d(TAG, "✅ WEBSOCKET DE UBICACIONES CONECTADO")
            Log.d(TAG, "✅ Estado: CONNECTED")
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
            // ✅ MARCAR COMO DESCONECTANDO
            isWebSocketConnected = false

            Log.d(TAG, "⚠️ WebSocket cerrándose: $code - $reason")
            Log.d(TAG, "⚠️ Estado: CLOSING")

            externalListener?.onClosing(webSocket, code, reason)
            broadcastListeners.forEach { it.onClosing(webSocket, code, reason) }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            // ✅ MARCAR COMO CERRADO
            isWebSocketConnected = false

            Log.d(TAG, "🔒 WebSocket cerrado: $code - $reason")
            Log.d(TAG, "🔒 Estado: CLOSED")

            externalListener?.onClosed(webSocket, code, reason)
            broadcastListeners.forEach { it.onClosed(webSocket, code, reason) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            // ✅ MARCAR COMO DESCONECTADO
            isWebSocketConnected = false

            Log.e(TAG, "❌ ════════════════════════════════════════")
            Log.e(TAG, "❌ WEBSOCKET FALLÓ")
            Log.e(TAG, "❌ Error: ${t.message}")
            Log.e(TAG, "❌ Código: ${response?.code}")
            Log.e(TAG, "❌ Estado: FAILED")
            Log.e(TAG, "❌ ════════════════════════════════════════")

            externalListener?.onFailure(webSocket, t, response)
            broadcastListeners.forEach { it.onFailure(webSocket, t, response) }
        }
    }

    @Synchronized
    fun initialize(context: Context) {
        if (tokenChangeListener != null) {
            Log.d(TAG, "⚠️ Ya está inicializado, ignorando llamada duplicada")
            return
        }

        sessionManager = SessionManager.getInstance(context.applicationContext)

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

    // Cambiar la firma de la función
    fun connectGlobal(baseUrl: String, token: String, grupoId: Int) {  // ← Agregar grupoId
        if (isConnected()) {
            Log.d(TAG, "⚠️ Ya está conectado, actualizando token...")
            updateToken(token)
            return
        }

        val wsUrl = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/ws/grupos/$grupoId/ubicaciones?token=$token"  // ← Cambiar URL

        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "🔌 CONECTANDO GLOBALMENTE")
        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "   URL: ${wsUrl.substringBefore("?token=")}")
        Log.d(TAG, "   Token: ${token.take(20)}...")
        Log.d(TAG, "   Grupo ID: $grupoId")

        currentToken = token
        connectInternal(wsUrl)
    }

    fun connect(url: String, listener: WebSocketListener) {
        if (isConnected()) {
            Log.d(TAG, "✅ Ya conectado, registrando listener externo")
            externalListener = listener
            return
        }

        Log.d(TAG, "🔌 Conectando con listener externo...")
        Log.d(TAG, "   URL: ${url.substringBefore("?token=")}")

        externalListener = listener
        connectInternal(url)
    }

    private fun connectInternal(url: String) {
        // Limpiar conexión anterior si existe
        if (webSocket != null) {
            Log.d(TAG, "🧹 Limpiando conexión anterior...")
            webSocket?.close(1000, "Reconectando")
            webSocket = null
            isWebSocketConnected = false
        }

        client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(10, TimeUnit.SECONDS) // ✅ Añadir timeout de conexión
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        Log.d(TAG, "🔌 Iniciando conexión WebSocket...")
        webSocket = client?.newWebSocket(request, internalListener)
    }

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
            if (!isConnected()) {
                Log.w(TAG, "⚠️ No conectado - no se puede enviar mensaje")
                return false
            }

            val sent = webSocket?.send(message) ?: false
            if (sent) {
                Log.v(TAG, "📤 Mensaje enviado: ${message.take(50)}")
            } else {
                Log.w(TAG, "⚠️ Error al enviar mensaje")
            }
            sent
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al enviar: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // ✅ CORRECCIÓN CRÍTICA: Verificar estado real
    fun isConnected(): Boolean {
        val connected = webSocket != null && isWebSocketConnected
        Log.v(TAG, "🔍 Estado: webSocket=${webSocket != null}, connected=$isWebSocketConnected -> $connected")
        return connected
    }

    fun close() {
        Log.d(TAG, "🔒 ════════════════════════════════════════")
        Log.d(TAG, "🔒 CERRANDO WEBSOCKET DE UBICACIONES")
        Log.d(TAG, "🔒 ════════════════════════════════════════")

        // Marcar como desconectado
        isWebSocketConnected = false

        // Desregistrar listener de tokens
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

    // ✅ Método de diagnóstico
    fun getConnectionStatus(): String {
        return """
            WebSocket object: ${webSocket != null}
            Connection flag: $isWebSocketConnected
            Token available: ${currentToken != null}
            Listeners: ${broadcastListeners.size}
        """.trimIndent()
    }
}