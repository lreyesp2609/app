package com.example.app

import android.app.Application
import android.util.Log
import com.example.app.network.RetrofitClient
import com.example.app.websocket.WebSocketLocationManager
import com.example.app.websocket.WebSocketManager
import com.example.app.utils.SessionManager
import com.example.app.websocket.NotificationWebSocketManager
import com.google.gson.Gson

class RecuerdaGoApplication : Application() {

    companion object {
        private const val TAG = "RecuerdaGoApp"
    }

    override fun onCreate() {
        super.onCreate()

        repeat(3) {
            Log.e(TAG, "════════════════════════════════════════")
            Log.e(TAG, "🚀🚀🚀 APLICACIÓN INICIADA 🚀🚀🚀")
            Log.e(TAG, "════════════════════════════════════════")
        }

        // 🔥 Inicializar RetrofitClient con contexto
        RetrofitClient.init(this)

        Log.d(TAG, "✅ RetrofitClient inicializado con AuthInterceptor")
        Log.d(TAG, "✅ ========================================")

        val sessionManager = SessionManager.getInstance(this)

        // 🔥 REGISTRAR EL LISTENER INMEDIATAMENTE
        Log.e(TAG, "🔧 Registrando listener de tokens...")

        sessionManager.addTokenChangeListener { newToken ->
            Log.e(TAG, "════════════════════════════════════════")
            Log.e(TAG, "🔔🔔🔔 TOKEN ACTUALIZADO GLOBALMENTE 🔔🔔🔔")
            Log.e(TAG, "════════════════════════════════════════")
            Log.e(TAG, "   Nuevo token: ${newToken.take(20)}...")

            try {
                // 🔄 MENSAJE PARA CHATS (usa "action")
                val mensajeChats = mapOf(
                    "action" to "refresh_token",
                    "data" to mapOf("token" to newToken)
                )

                // 🔄 MENSAJE PARA UBICACIONES (usa "type")
                val mensajeUbicaciones = mapOf(
                    "type" to "refresh_token",
                    "token" to newToken
                )

                // 🆕 MENSAJE PARA NOTIFICACIONES (usa "action")
                val mensajeNotificaciones = mapOf(
                    "action" to "refresh_token",
                    "data" to mapOf("token" to newToken)
                )

                var enviados = 0

                // Enviar a WebSocket de chats
                if (WebSocketManager.isConnected()) {
                    val mensajeJson = Gson().toJson(mensajeChats)
                    if (WebSocketManager.send(mensajeJson)) {
                        enviados++
                        Log.e(TAG, "✅ Token enviado al WebSocket de chats")
                    }
                } else {
                    Log.e(TAG, "ℹ️ WebSocket de chats no conectado")
                }

                // Enviar a WebSocket de ubicaciones
                if (WebSocketLocationManager.isConnected()) {
                    val mensajeJson = Gson().toJson(mensajeUbicaciones)
                    if (WebSocketLocationManager.send(mensajeJson)) {
                        enviados++
                        Log.e(TAG, "✅ Token enviado al WebSocket de ubicaciones")
                    }
                } else {
                    Log.e(TAG, "ℹ️ WebSocket de ubicaciones no conectado")
                }

                // 🆕 Enviar a WebSocket de notificaciones
                if (NotificationWebSocketManager.isConnected()) {
                    val mensajeJson = Gson().toJson(mensajeNotificaciones)
                    if (NotificationWebSocketManager.send(mensajeJson)) {
                        enviados++
                        Log.e(TAG, "✅ Token enviado al WebSocket de notificaciones")
                    }
                } else {
                    Log.e(TAG, "ℹ️ WebSocket de notificaciones no conectado")
                }

                Log.e(TAG, "📊 Resumen: Token enviado a $enviados WebSockets")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al enviar token: ${e.message}")
                e.printStackTrace()
            }
        }

        Log.e(TAG, "════════════════════════════════════════")
        Log.e(TAG, "✅ LISTENER GLOBAL REGISTRADO PERMANENTEMENTE")
        Log.e(TAG, "✅ Total de listeners: ${sessionManager.getListenerCount()}")
        Log.e(TAG, "════════════════════════════════════════")
    }
}