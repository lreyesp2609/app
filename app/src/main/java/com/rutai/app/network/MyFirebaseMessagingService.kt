package com.rutai.app.network

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rutai.app.repository.AuthRepository
import com.rutai.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 📡 MyFirebaseMessagingService - Maneja notificaciones y refresco silencioso.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        private const val TAG = "FCM_Service"
    }

    /**
     * Se activa cuando llega un mensaje de FCM.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // 1️⃣ Verificar si es una instrucción de refresco silencioso
        val type = remoteMessage.data["type"]
        if (type == "SILENT_TOKEN_REFRESH") {
            Log.d(TAG, "🔄 Instrucción SILENT_TOKEN_REFRESH recibida")
            
            val sessionManager = SessionManager.getInstance(applicationContext)
            val refreshToken = sessionManager.getRefreshToken()
            
            if (!refreshToken.isNullOrEmpty() && sessionManager.isLoggedIn()) {
                serviceScope.launch {
                    AuthRepository(applicationContext).refreshToken(refreshToken)
                        .onSuccess { response ->
                            // saveTokens ya dispara notifyTokenListeners() internamente
                            sessionManager.saveTokens(response.accessToken, response.refreshToken)
                            Log.d(TAG, "✅ JWT refrescado silenciosamente vía FCM")
                        }
                        .onFailure { e ->
                            Log.e(TAG, "❌ Falló el refresco silencioso vía FCM: ${e.message}")
                        }
                }
            }
        }
    }

    /**
     * Se activa cuando Google genera un nuevo token FCM para el dispositivo.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🆕 Nuevo token FCM generado: ${token.take(8)}...")

        val sessionManager = SessionManager.getInstance(applicationContext)
        
        // 1️⃣ Guardar siempre localmente (para el Login posterior si no está logueado)
        sessionManager.saveFcmToken(token)

        // 2️⃣ Si el usuario ya tiene sesión, enviarlo al servidor inmediatamente
        if (sessionManager.isLoggedIn()) {
            val accessToken = sessionManager.getAccessToken()
            if (accessToken != null) {
                serviceScope.launch {
                    val request = mapOf(
                        "token" to token,
                        "dispositivo" to "android"
                    )
                    AuthRepository(applicationContext).enviarTokenFCM("Bearer $accessToken", request)
                        .onSuccess { Log.d(TAG, "✅ Nuevo token FCM registrado en el servidor") }
                        .onFailure { e -> Log.e(TAG, "❌ Error al registrar nuevo token FCM: ${e.message}") }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
