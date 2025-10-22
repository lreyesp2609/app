package com.example.app

import android.app.Application
import android.util.Log
import com.example.app.network.WebSocketManager
import com.example.app.utils.SessionManager
import com.google.gson.Gson

class RecuerdaGoApplication : Application() {

    companion object {
        private const val TAG = "🔌WS_SessionManager"
    }

    override fun onCreate() {
        super.onCreate()

        // 🔍 Logs MUY visibles con Log.e para asegurar que se vean
        repeat(3) {
            Log.e(TAG, "════════════════════════════════════════")
            Log.e(TAG, "🚀🚀🚀 APLICACIÓN INICIADA 🚀🚀🚀")
            Log.e(TAG, "════════════════════════════════════════")
        }

        // 🆕 Registrar listener GLOBAL de tokens para WebSocket
        val sessionManager = SessionManager.getInstance(this)

        sessionManager.addTokenChangeListener { newToken ->
            Log.e(TAG, "════════════════════════════════════════")
            Log.e(TAG, "🔔🔔🔔 TOKEN ACTUALIZADO GLOBALMENTE 🔔🔔🔔")
            Log.e(TAG, "════════════════════════════════════════")
            Log.e(TAG, "   Nuevo token: ${newToken.take(20)}...")

            // 🔄 Enviar el nuevo token al WebSocket si está conectado
            try {
                val mensaje = mapOf(
                    "action" to "refresh_token",
                    "data" to mapOf("token" to newToken)
                )
                val mensajeJson = Gson().toJson(mensaje)

                if (WebSocketManager.isConnected()) {
                    WebSocketManager.send(mensajeJson)
                    Log.e(TAG, "✅ Token enviado al WebSocket activo")
                } else {
                    Log.e(TAG, "ℹ️ WebSocket no conectado, token se usará en próxima conexión")
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