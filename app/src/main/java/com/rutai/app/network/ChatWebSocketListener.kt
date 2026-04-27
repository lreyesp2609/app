package com.rutai.app.network

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class ChatWebSocketListener(
    private val onMessageReceived: (String) -> Unit,
    private val onConnected: () -> Unit = {},
    private val onDisconnected: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        println("✅ WebSocket conectado correctamente")
        onConnected()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        println("📩 Mensaje recibido: $text")
        onMessageReceived(text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        println("⚠️ WebSocket cerrándose: $code - $reason")
        webSocket.close(1000, null)
        onDisconnected()
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        println("❌ WebSocket cerrado: $code - $reason")
        onDisconnected()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        val errorMsg = "Error WebSocket: ${t.message}"
        println("❌ $errorMsg")
        onError(errorMsg)
        onDisconnected()
    }
}