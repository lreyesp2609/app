package com.example.app.websocket

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.app.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Singleton para gestionar el WebSocket de ubicaciones en tiempo real (Global)
 */
object WebSocketLocationManager {
    private const val TAG = "WS_LocationManager"
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    
    private var savedUrl: String? = null
    private var savedToken: String? = null
    private var isInitialized = false

    private val internalListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "✅ ════════════════════════════════════════")
            Log.d(TAG, "✅ WEBSOCKET DE UBICACIONES CONECTADO")
            Log.d(TAG, "✅ ════════════════════════════════════════")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.v(TAG, "📨 Mensaje recibido")
            // Aquí se podrían procesar actualizaciones globales de ubicación
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "❌ Error en WebSocket de ubicaciones: ${t.message}")
            
            if (savedUrl != null && savedToken != null) {
                val delay = if (response?.code == 403) 30000L else 10000L
                Log.d(TAG, "🔄 Reintentando conexión en ${delay/1000}s...")
                scheduleReconnect(delay)
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "🔒 WebSocket cerrado: $code - $reason")
            if (code != 1000 && savedUrl != null && savedToken != null) {
                scheduleReconnect()
            }
        }
    }

    private fun scheduleReconnect(delay: Long = 10000L) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isConnected() && savedUrl != null && savedToken != null) {
                Log.d(TAG, "🔄 Ejecutando reconexión programada...")
                connect(savedUrl!!, savedToken!!)
            }
        }, delay)
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        
        val sessionManager = SessionManager.getInstance(context)
        sessionManager.addTokenChangeListener { newToken ->
            Log.d(TAG, "🔄 Token actualizado detectado")
            savedToken = newToken
            if (isConnected()) {
                updateToken(newToken)
            } else if (savedUrl != null) {
                Log.d(TAG, "🔄 Socket caído, intentando conectar con nuevo token...")
                connect(savedUrl!!, newToken)
            }
        }
        isInitialized = true
        Log.d(TAG, "✅ WebSocketLocationManager inicializado")
    }

    /**
     * Verificar salud y reconectar si es necesario (onResume)
     */
    fun checkHealth() {
        if (!isConnected() && savedUrl != null && savedToken != null) {
            Log.d(TAG, "🏥 Salud: Socket de ubicaciones caído, reconectando...")
            connect(savedUrl!!, savedToken!!)
        } else {
            Log.v(TAG, "🏥 Salud: Socket de ubicaciones OK")
        }
    }

    fun connect(url: String, token: String) {
        savedUrl = url
        savedToken = token

        if (isConnected()) {
            updateToken(token)
            return
        }

        Log.d(TAG, "🔌 Conectando a: $url")
        
        // Limpiar cliente anterior
        client?.dispatcher?.executorService?.shutdown()

        client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder()
            .url("$url?token=$token")
            .build()

        webSocket = client?.newWebSocket(request, internalListener)
    }

    fun updateToken(newToken: String) {
        if (!isConnected()) return
        
        val message = JSONObject().apply {
            put("type", "token_refresh")
            put("token", newToken)
        }.toString()
        
        send(message)
    }

    fun send(message: String): Boolean {
        return webSocket?.send(message) ?: false
    }

    fun isConnected(): Boolean = webSocket != null

    fun close() {
        Log.d(TAG, "🔒 Cerrando conexión")
        webSocket?.close(1000, "Cierre normal")
        webSocket = null
        savedUrl = null
        savedToken = null
        client?.dispatcher?.executorService?.shutdown()
        client = null
    }
}