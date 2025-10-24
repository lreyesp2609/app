package com.example.app

import android.app.Application
import android.util.Log
import com.example.app.network.WebSocketLocationManager
import com.example.app.network.WebSocketManager
import com.example.app.utils.SessionManager
import com.google.gson.Gson

class RecuerdaGoApplication : Application() {

    companion object {
        private const val TAG = "🔌WS_SessionManager"
    }

    override fun onCreate() {
        super.onCreate()

        repeat(3) {
            Log.e(TAG, "════════════════════════════════════════")
            Log.e(TAG, "🚀🚀🚀 APLICACIÓN INICIADA 🚀🚀🚀")
            Log.e(TAG, "════════════════════════════════════════")
        }

        val sessionManager = SessionManager.getInstance(this)

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

                // Enviar a WebSocket de chats
                if (WebSocketManager.isConnected()) {
                    val mensajeJson = Gson().toJson(mensajeChats)
                    WebSocketManager.send(mensajeJson)
                    Log.e(TAG, "✅ Token enviado al WebSocket activo")
                } else {
                    Log.e(TAG, "ℹ️ WebSocket no conectado, token se usará en próxima conexión")
                }

                // Enviar a WebSocket de ubicaciones
                if (WebSocketLocationManager.isConnected()) {
                    val mensajeJson = Gson().toJson(mensajeUbicaciones)
                    WebSocketLocationManager.send(mensajeJson)
                    Log.e(TAG, "✅ Token enviado al WebSocket de ubicaciones")
                    Log.e(TAG, "   Formato: {\"type\":\"refresh_token\",\"token\":\"...\"}")
                } else {
                    Log.e(TAG, "ℹ️ WebSocket de ubicaciones no conectado")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al enviar token: ${e.message}")
                e.printStackTrace()
            }
        }

        Log.e(TAG, "════════════════════════════════════════")
        Log.e(TAG, "✅ LISTENER GLOBAL REGISTRADO PERMANENTEMENTE")
        Log.e(TAG, "════════════════════════════════════════")
    }
}