package com.example.app.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import java.util.concurrent.TimeUnit
import com.google.gson.Gson

object WebSocketLocationManager {
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var externalListener: WebSocketListener? = null

    // 🆕 Lista de listeners para broadcast
    private val broadcastListeners = mutableListOf<WebSocketListener>()

    private var currentToken: String? = null
    private val gson = Gson()
    private const val TAG = "📍WS_SessionManager"

    // 🆕 Listener interno que hace BROADCAST a todos los listeners
    private val internalListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "✅ WebSocket de ubicaciones conectado")
            externalListener?.onOpen(webSocket, response)

            // 📢 Broadcast a todos los listeners
            broadcastListeners.forEach { it.onOpen(webSocket, response) }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.v(TAG, "📨 Mensaje recibido: ${text.take(100)}")

            try {
                val mensaje = gson.fromJson(text, Map::class.java) as? Map<*, *>
                val type = mensaje?.get("type") as? String

                // 🔄 Detectar mensajes relacionados con tokens
                if (type == "refresh_token" || type == "token_refreshed") {
                    Log.d(TAG, "🔄 ════════════════════════════════════════")
                    Log.d(TAG, "🔄 MENSAJE DE TOKEN DETECTADO")
                    Log.d(TAG, "🔄 ════════════════════════════════════════")
                    Log.d(TAG, "   Tipo: $type")

                    if (type == "token_refreshed") {
                        val msg = mensaje["message"] as? String
                        Log.d(TAG, "✅ ════════════════════════════════════════")
                        Log.d(TAG, "✅ SERVIDOR CONFIRMÓ ACTUALIZACIÓN DE TOKEN")
                        Log.d(TAG, "✅ ════════════════════════════════════════")
                        Log.d(TAG, "   Mensaje: $msg")
                        return
                    }

                    if (type == "refresh_token") {
                        val newToken = mensaje["token"] as? String

                        if (newToken != null) {
                            currentToken = newToken
                            Log.d(TAG, "✅ ════════════════════════════════════════")
                            Log.d(TAG, "✅ TOKEN ACTUALIZADO EXITOSAMENTE")
                            Log.d(TAG, "✅ ════════════════════════════════════════")
                            Log.d(TAG, "   Nuevo token: ${newToken.take(20)}...")
                        } else {
                            Log.e(TAG, "❌ ════════════════════════════════════════")
                            Log.e(TAG, "❌ TOKEN NO ENCONTRADO EN MENSAJE")
                            Log.e(TAG, "❌ ════════════════════════════════════════")
                            Log.e(TAG, "   Mensaje completo: $mensaje")
                        }
                        return
                    }
                }
            } catch (e: Exception) {
                Log.v(TAG, "ℹ️  Mensaje no es un comando interno, propagando...")
            }

            // 📤 Propagar mensaje a TODOS los listeners
            externalListener?.onMessage(webSocket, text)
            broadcastListeners.forEach {
                try {
                    it.onMessage(webSocket, text)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error en listener broadcast: ${e.message}")
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
            Log.e(TAG, "❌ Error en WebSocket: ${t.message}")
            t.printStackTrace()
            externalListener?.onFailure(webSocket, t, response)
            broadcastListeners.forEach { it.onFailure(webSocket, t, response) }
        }
    }

    fun connect(url: String, listener: WebSocketListener) {
        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "🔌 CONECTANDO WEBSOCKET DE UBICACIONES")
        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "   URL: $url")

        externalListener = listener

        client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client?.newWebSocket(request, internalListener)

        Log.d(TAG, "✅ Listener interno configurado para interceptar mensajes")
    }

    // 🆕 Método para que el ViewModel se suscriba sin reconectar
    fun addBroadcastListener(listener: WebSocketListener) {
        if (!broadcastListeners.contains(listener)) {
            broadcastListeners.add(listener)
            Log.d(TAG, "📢 Listener agregado al broadcast. Total: ${broadcastListeners.size}")
        }
    }

    // 🆕 Método para que el ViewModel se desuscriba
    fun removeBroadcastListener(listener: WebSocketListener) {
        val removed = broadcastListeners.remove(listener)
        if (removed) {
            Log.d(TAG, "📢 Listener removido del broadcast. Total: ${broadcastListeners.size}")
        }
    }

    fun send(message: String): Boolean {
        return try {
            val sent = webSocket?.send(message) ?: false
            if (sent) {
                Log.v(TAG, "📤 Mensaje enviado correctamente")
                Log.v(TAG, "   Contenido: ${message.take(100)}...")
            } else {
                Log.w(TAG, "⚠️ No se pudo enviar mensaje (WebSocket no conectado)")
            }
            sent
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al enviar mensaje: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun isConnected(): Boolean {
        val connected = webSocket != null
        Log.v(TAG, "🔍 Estado de conexión: ${if (connected) "CONECTADO" else "DESCONECTADO"}")
        return connected
    }

    fun close() {
        Log.d(TAG, "🔒 ════════════════════════════════════════")
        Log.d(TAG, "🔒 CERRANDO WEBSOCKET DE UBICACIONES")
        Log.d(TAG, "🔒 ════════════════════════════════════════")

        webSocket?.close(1000, "Cliente cerró la conexión")
        webSocket = null
        externalListener = null
        broadcastListeners.clear()
        currentToken = null

        client?.dispatcher?.executorService?.shutdown()
        client = null

        Log.d(TAG, "✅ WebSocket cerrado y recursos liberados")
    }

    fun getCurrentToken(): String? {
        return currentToken
    }
}