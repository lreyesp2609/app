package com.rutai.app

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import com.rutai.app.network.RetrofitClient
import com.rutai.app.websocket.WebSocketLocationManager
import com.rutai.app.websocket.WebSocketManager
import com.rutai.app.utils.SessionManager
import com.rutai.app.websocket.NotificationWebSocketManager
import com.rutai.app.workers.TokenRefreshWorker
import com.rutai.app.repository.AuthRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File
import java.util.concurrent.TimeUnit

class RecuerdaGoApplication : Application(), DefaultLifecycleObserver {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "RecuerdaGoApp"
        private const val TOKEN_WORK_NAME = "TokenRefreshWork"
    }

    override fun onCreate() {
        super<Application>.onCreate()

        Log.d(TAG, "🚀 Aplicación Iniciada - Configurando servicios de Auth")

        // 1️⃣ Inicializar Retrofit con el Interceptor corregido
        RetrofitClient.init(this)

        // 2️⃣ Configurar Mapas
        setupOsmdroid()

        val sessionManager = SessionManager.getInstance(this)

        // 3️⃣ Registrar listener global para WebSockets
        setupTokenChangeListener(sessionManager)

        // 4️⃣ Programar refresco en background (cada 15 min)
        setupTokenRefreshWorker()

        // 5️⃣ Observar ciclo de vida (Foreground/Background)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun setupOsmdroid() {
        val userAgent = "${BuildConfig.APPLICATION_ID}/1.0"
        Configuration.getInstance().userAgentValue = userAgent
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        val osmConfig = Configuration.getInstance()
        osmConfig.osmdroidBasePath = File(cacheDir, "osmdroid")
        osmConfig.osmdroidTileCache = File(osmConfig.osmdroidBasePath, "tiles")
    }

    private fun setupTokenRefreshWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val tokenWorkRequest = PeriodicWorkRequestBuilder<TokenRefreshWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TOKEN_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            tokenWorkRequest
        )
    }

    private fun setupTokenChangeListener(sessionManager: SessionManager) {
        sessionManager.addTokenChangeListener { newToken ->
            Log.d(TAG, "🔔 Token renovado: Actualizando WebSockets...")
            val gson = Gson()

            // Chat WebSocket
            if (WebSocketManager.isConnected()) {
                val msg = gson.toJson(mapOf("action" to "refresh_token", "data" to mapOf("token" to newToken)))
                WebSocketManager.send(msg)
            }

            // Location WebSocket
            if (WebSocketLocationManager.isConnected()) {
                val msg = gson.toJson(mapOf("type" to "refresh_token", "token" to newToken))
                WebSocketLocationManager.send(msg)
            }

            // Notifications WebSocket
            if (NotificationWebSocketManager.isConnected()) {
                val msg = gson.toJson(mapOf("action" to "refresh_token", "data" to mapOf("token" to newToken)))
                NotificationWebSocketManager.send(msg)
            }
        }
    }

    // 🔄 EVENTOS DE CICLO DE VIDA (DefaultLifecycleObserver)

    override fun onStart(owner: LifecycleOwner) {
        // super.onStart(owner) es opcional (es una interfaz con default vacío)
        Log.d(TAG, "📱 App vuelve al primer plano")

        val sessionManager = SessionManager.getInstance(this)

        if (sessionManager.isLoggedIn()) {
            applicationScope.launch {
                if (sessionManager.isTokenExpiringSoon(marginMinutes = 5)) {
                    Log.w(TAG, "⚠️ Token crítico detectado al volver. Refrescando...")
                    sessionManager.getRefreshToken()?.let { rt ->
                        AuthRepository(applicationContext).refreshToken(rt).onSuccess { res ->
                            sessionManager.saveTokens(res.accessToken, res.refreshToken)
                            Log.d(TAG, "✅ Token recuperado con éxito al inicio")
                        }.onFailure { e ->
                            Log.e(TAG, "❌ Error al recuperar sesión: ${e.message}")
                        }
                    }
                }

                sessionManager.startAutoRefreshIfNeeded {
                    sendBroadcast(Intent("com.rutai.app.FORCE_LOGOUT"))
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "📉 App en background. Deteniendo loop proactivo.")
        SessionManager.getInstance(this).stopAutoRefresh()
    }
}