package com.example.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.app.BuildConfig
import com.example.app.MainActivity
import com.example.app.R
import com.example.app.utils.SessionManager
import com.example.app.websocket.NotificationWebSocketManager
import com.example.app.websocket.WebSocketLocationManager
import com.example.app.websocket.WebSocketManager

class WebSocketService : Service() {
    private var isRunning = false
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "🔌WebSocketService"
        const val ACTION_START = "START_WEBSOCKETS"
        const val ACTION_STOP = "STOP_WEBSOCKETS"
        private const val NOTIFICATION_ID = 2000
        private const val CHANNEL_ID = "websocket_service"

        fun start(context: Context) {
            val intent = Intent(context, WebSocketService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "📤 Comando de inicio enviado")
        }

        fun stop(context: Context) {
            val intent = Intent(context, WebSocketService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
            Log.d(TAG, "📤 Comando de detención enviado")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🏗️ ════════════════════════════════════════")
        Log.d(TAG, "🏗️ SERVICIO DE WEBSOCKETS CREADO")
        Log.d(TAG, "🏗️ ════════════════════════════════════════")

        sessionManager = SessionManager.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isRunning) {
                    Log.d(TAG, "🚀 ════════════════════════════════════════")
                    Log.d(TAG, "🚀 INICIANDO SERVICIO DE WEBSOCKETS")
                    Log.d(TAG, "🚀 ════════════════════════════════════════")

                    startForeground(NOTIFICATION_ID, createNotification())
                    connectWebSockets()
                    isRunning = true

                    Log.d(TAG, "✅ Servicio iniciado correctamente")
                } else {
                    Log.d(TAG, "ℹ️ Servicio ya está corriendo")
                }
            }
            ACTION_STOP -> {
                Log.d(TAG, "🛑 ════════════════════════════════════════")
                Log.d(TAG, "🛑 DETENIENDO SERVICIO DE WEBSOCKETS")
                Log.d(TAG, "🛑 ════════════════════════════════════════")

                disconnectWebSockets()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }

                stopSelf()
                isRunning = false

                Log.d(TAG, "✅ Servicio detenido correctamente")
            }
        }

        // START_STICKY: Si Android mata el servicio, se reiniciará automáticamente
        return START_STICKY
    }

    private fun connectWebSockets() {
        val token = sessionManager.getAccessToken()
        if (token == null) {
            Log.e(TAG, "❌ ════════════════════════════════════════")
            Log.e(TAG, "❌ NO HAY TOKEN DISPONIBLE")
            Log.e(TAG, "❌ No se pueden conectar los WebSockets")
            Log.e(TAG, "❌ ════════════════════════════════════════")
            return
        }

        val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")

        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "🔌 CONECTANDO TODOS LOS WEBSOCKETS")
        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "   Base URL: $baseUrl")
        Log.d(TAG, "   Token: ${token.take(20)}...")

        try {
            // 1️⃣ Conectar WebSocket de Notificaciones
            Log.d(TAG, "🔔 Conectando WebSocket de Notificaciones...")
            NotificationWebSocketManager.connect(baseUrl, token)

            // 2️⃣ Conectar WebSocket de Chat
            Log.d(TAG, "💬 Conectando WebSocket de Chat...")
            WebSocketManager.connectGlobal(baseUrl, token)

            // 3️⃣ Conectar WebSocket de Ubicaciones
            Log.d(TAG, "📍 Conectando WebSocket de Ubicaciones...")
            WebSocketLocationManager.connectGlobal(baseUrl, token)

            Log.d(TAG, "✅ ════════════════════════════════════════")
            Log.d(TAG, "✅ TODOS LOS WEBSOCKETS CONECTADOS")
            Log.d(TAG, "✅ ════════════════════════════════════════")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al conectar WebSockets: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun disconnectWebSockets() {
        Log.d(TAG, "🔒 ════════════════════════════════════════")
        Log.d(TAG, "🔒 DESCONECTANDO TODOS LOS WEBSOCKETS")
        Log.d(TAG, "🔒 ════════════════════════════════════════")

        try {
            // Cerrar todos los WebSockets
            NotificationWebSocketManager.close()
            WebSocketManager.close()
            WebSocketLocationManager.close()

            Log.d(TAG, "✅ Todos los WebSockets desconectados")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al desconectar WebSockets: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Conexión en tiempo real",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene la conexión con el servidor activa"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "📢 Canal de notificación creado")
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RecuerdaGo")
            .setContentText("Conectado al servidor")
            .setSmallIcon(R.drawable.ic_notification) // ⚠️ Cambia por tu ícono
            .setOngoing(true) // No se puede deslizar para cerrar
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "💀 ════════════════════════════════════════")
        Log.d(TAG, "💀 SERVICIO DESTRUIDO")
        Log.d(TAG, "💀 ════════════════════════════════════════")

        disconnectWebSockets()
        isRunning = false
    }
}