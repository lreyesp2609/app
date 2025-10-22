package com.example.app.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import android.util.Log
import java.util.concurrent.TimeUnit

object WebSocketManager {
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private const val TAG = "🔌WS_WebSocketManager"

    fun connect(url: String, listener: WebSocketListener) {
        Log.d(TAG, "🔌 Conectando WebSocket...")

        client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client?.newWebSocket(request, listener)
    }

    fun send(message: String): Boolean {
        return try {
            val sent = webSocket?.send(message) ?: false
            if (sent) {
                Log.d(TAG, "📤 Mensaje enviado correctamente")
            } else {
                Log.w(TAG, "⚠️ No se pudo enviar el mensaje (WebSocket no conectado)")
            }
            sent
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al enviar mensaje: ${e.message}")
            false
        }
    }

    // 🆕 Verificar si el WebSocket está conectado
    fun isConnected(): Boolean {
        val connected = webSocket != null
        Log.v(TAG, "🔍 Estado de conexión: ${if (connected) "CONECTADO" else "DESCONECTADO"}")
        return connected
    }

    fun close() {
        Log.d(TAG, "🔒 Cerrando WebSocket")
        webSocket?.close(1000, "Cliente cerró la conexión")
        webSocket = null
        client?.dispatcher?.executorService?.shutdown()
        client = null
    }
}