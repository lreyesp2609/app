package com.example.app.services

import android.util.Log
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class LocationWebSocketListener(
    private val onUbicacionRecibida: (String) -> Unit,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit,
    private val onError: (String) -> Unit
) : WebSocketListener() {

    companion object {
        private const val TAG = "📍WS_SessionManager"
    }

    override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
        Log.d(TAG, "✅ WebSocket de ubicaciones abierto")
        onConnected()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.v(TAG, "📩 Mensaje recibido: $text")
        onUbicacionRecibida(text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.w(TAG, "⚠️ WebSocket cerrando: [$code] $reason")
        webSocket.close(1000, null)
        onDisconnected()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
        Log.e(TAG, "❌ Error en WebSocket: ${t.message}")
        t.printStackTrace()
        onError(t.message ?: "Error desconocido")
        onDisconnected()
    }
}