package com.example.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.app.BuildConfig
import com.example.app.R
import com.example.app.repository.AuthRepository
import com.example.app.websocket.WebSocketLocationManager
import com.example.app.utils.SessionManager
import com.google.android.gms.location.*
import com.google.gson.Gson
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlinx.coroutines.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentGrupoId: Int = -1

    // 🆕 Propiedades para auto-refresh
    private val sessionManager by lazy { SessionManager.getInstance(applicationContext) }
    private val authRepository = AuthRepository()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "📍LocationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_service_channel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_GRUPO_ID = "grupo_id"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🎬 ════════════════════════════════════════")
        Log.d(TAG, "🎬 LOCATIONSERVICE CREADO")
        Log.d(TAG, "🎬 ════════════════════════════════════════")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    enviarUbicacion(location)
                }
            }
        }

        // 🆕 Inicializar WebSocket Manager
        WebSocketLocationManager.initialize(applicationContext)

        // 🆕 Iniciar auto-refresh de tokens
        startTokenAutoRefresh()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentGrupoId = intent.getIntExtra(EXTRA_GRUPO_ID, -1)

                if (currentGrupoId == -1) {
                    Log.e(TAG, "❌ No se proporcionó grupo_id")
                    stopSelf()
                    return START_NOT_STICKY
                }

                Log.d(TAG, "▶️ ════════════════════════════════════════")
                Log.d(TAG, "▶️ INICIANDO SERVICIO PARA GRUPO $currentGrupoId")
                Log.d(TAG, "▶️ ════════════════════════════════════════")

                startForeground(NOTIFICATION_ID, createNotification())
                conectarWebSocket()
                startLocationUpdates()
            }
            ACTION_STOP -> {
                Log.d(TAG, "⏹️ ════════════════════════════════════════")
                Log.d(TAG, "⏹️ DETENIENDO SERVICIO DE UBICACIÓN")
                Log.d(TAG, "⏹️ ════════════════════════════════════════")

                stopLocationUpdates()
                desconectarWebSocket()
                stopSelf()
            }
        }

        // 🔥 CRÍTICO: Reiniciar servicio si Android lo mata
        return START_STICKY
    }

    // 🆕 AUTO-REFRESH DE TOKENS DESDE EL SERVICIO
    private fun startTokenAutoRefresh() {
        serviceScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000) // 5 minutos

                val refreshToken = sessionManager.getRefreshToken()

                if (refreshToken != null) {
                    Log.d(TAG, "🔄 ════════════════════════════════════════")
                    Log.d(TAG, "🔄 AUTO-REFRESH DESDE LOCATION SERVICE")
                    Log.d(TAG, "🔄 ════════════════════════════════════════")

                    try {
                        val result = authRepository.refreshToken(refreshToken)

                        result.fold(
                            onSuccess = { response ->
                                Log.d(TAG, "✅ Token renovado exitosamente")
                                Log.d(TAG, "   Nuevo token: ${response.accessToken.take(20)}...")

                                // 🔥 CRÍTICO: Guardar tokens notifica automáticamente al WebSocket
                                sessionManager.saveTokens(
                                    response.accessToken,
                                    response.refreshToken
                                )

                                Log.d(TAG, "✅ ════════════════════════════════════════")
                                Log.d(TAG, "✅ TOKEN ACTUALIZADO Y WEBSOCKET NOTIFICADO")
                                Log.d(TAG, "✅ ════════════════════════════════════════")
                            },
                            onFailure = { error ->
                                Log.e(TAG, "❌ ════════════════════════════════════════")
                                Log.e(TAG, "❌ ERROR EN AUTO-REFRESH: ${error.message}")
                                Log.e(TAG, "❌ Deteniendo servicio por fallo de autenticación")
                                Log.e(TAG, "❌ ════════════════════════════════════════")

                                // Si falla el refresh, detener servicio
                                val stopIntent = Intent(this@LocationService, LocationService::class.java).apply {
                                    action = ACTION_STOP
                                }
                                startService(stopIntent)
                            }
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Excepción en auto-refresh: ${e.message}")
                        e.printStackTrace()
                    }
                } else {
                    Log.w(TAG, "⚠️ ════════════════════════════════════════")
                    Log.w(TAG, "⚠️ NO HAY REFRESH TOKEN")
                    Log.w(TAG, "⚠️ Deteniendo servicio")
                    Log.w(TAG, "⚠️ ════════════════════════════════════════")

                    val stopIntent = Intent(this@LocationService, LocationService::class.java).apply {
                        action = ACTION_STOP
                    }
                    startService(stopIntent)
                }
            }
        }
    }

    private fun conectarWebSocket() {
        val token = sessionManager.getAccessToken()

        if (token == null) {
            Log.e(TAG, "❌ No hay token disponible")
            return
        }

        // ✅ Si ya está conectado, no reconectar
        if (WebSocketLocationManager.isConnected()) {
            Log.d(TAG, "✅ WebSocket ya está conectado")
            return
        }

        val url = obtenerWebSocketUrl(currentGrupoId, token)
        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "🔌 CONECTANDO WEBSOCKET DESDE SERVICIO")
        Log.d(TAG, "🔌 ════════════════════════════════════════")
        Log.d(TAG, "   Grupo ID: $currentGrupoId")
        Log.d(TAG, "   URL: ${url.substringBefore("?token=")}")
        Log.d(TAG, "   Token: ${token.take(20)}...")

        // ✅ Conectar usando el Manager compartido
        WebSocketLocationManager.connect(url, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "✅ ════════════════════════════════════════")
                Log.d(TAG, "✅ WEBSOCKET CONECTADO DESDE SERVICIO")
                Log.d(TAG, "✅ ════════════════════════════════════════")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.v(TAG, "📨 Mensaje recibido en servicio: ${text.take(50)}")

                // Manejar ping/pong
                try {
                    val mensaje = Gson().fromJson(text, Map::class.java) as? Map<*, *>
                    if (mensaje?.get("type") == "ping") {
                        val pong = mapOf("type" to "pong")
                        WebSocketLocationManager.send(Gson().toJson(pong))
                    }
                } catch (e: Exception) {
                    // Ignorar
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "❌ ════════════════════════════════════════")
                Log.e(TAG, "❌ ERROR EN WEBSOCKET")
                Log.e(TAG, "❌ ${t.message}")
                Log.e(TAG, "❌ Response: ${response?.code} ${response?.message}")
                Log.e(TAG, "❌ ════════════════════════════════════════")

                // Si es 403, obtener nuevo token
                if (response?.code == 403) {
                    Log.w(TAG, "⚠️ Token rechazado (403), forzando refresh...")
                    serviceScope.launch {
                        val refreshToken = sessionManager.getRefreshToken()
                        if (refreshToken != null) {
                            val result = authRepository.refreshToken(refreshToken)
                            result.fold(
                                onSuccess = { refreshResponse ->
                                    Log.d(TAG, "✅ Token refrescado, reconectando...")
                                    sessionManager.saveTokens(
                                        refreshResponse.accessToken,
                                        refreshResponse.refreshToken
                                    )
                                    // Reintentar conexión con nuevo token
                                    delay(2000)
                                    conectarWebSocket()
                                },
                                onFailure = { error ->
                                    Log.e(TAG, "❌ Error al refrescar token: ${error.message}")
                                }
                            )
                        }
                    }
                } else {
                    // Reintentar conexión después de 5 segundos
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (currentGrupoId != -1) {
                            Log.d(TAG, "🔄 Reintentando conexión WebSocket...")
                            conectarWebSocket()
                        }
                    }, 5000)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "🔒 WebSocket cerrado: $code - $reason")
            }
        })
    }

    private fun desconectarWebSocket() {
        Log.d(TAG, "🔒 Desconectando WebSocket desde servicio")
        WebSocketLocationManager.close()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).apply {
            setMinUpdateIntervalMillis(5000L)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "✅ Actualizaciones de ubicación iniciadas (cada 5 segundos)")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permiso de ubicación no otorgado: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "⏸️ Actualizaciones de ubicación detenidas")
    }

    private fun enviarUbicacion(location: Location) {
        val ubicacion = mapOf(
            "type" to "ubicacion",
            "lat" to location.latitude,
            "lon" to location.longitude
        )

        val json = Gson().toJson(ubicacion)

        // ✅ Enviar usando el Manager
        val sent = WebSocketLocationManager.send(json)
        if (sent) {
            Log.v(TAG, "📤 Ubicación enviada: (${location.latitude}, ${location.longitude})")
        } else {
            Log.w(TAG, "⚠️ No se pudo enviar ubicación, WebSocket desconectado")
        }
    }

    private fun obtenerWebSocketUrl(grupoId: Int, token: String): String {
        val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")

        val wsUrl = when {
            baseUrl.startsWith("https://") -> baseUrl.replaceFirst("https://", "wss://")
            baseUrl.startsWith("http://") -> baseUrl.replaceFirst("http://", "ws://")
            else -> baseUrl
        }

        return "$wsUrl/grupos/ws/grupos/$grupoId/ubicaciones?token=$token"
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Compartiendo ubicación")
            .setContentText("Tu ubicación se está compartiendo con el grupo")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ubicación en tiempo real",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación para compartir ubicación en segundo plano"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "💀 ════════════════════════════════════════")
        Log.d(TAG, "💀 LOCATIONSERVICE DESTRUIDO")
        Log.d(TAG, "💀 ════════════════════════════════════════")

        // 🆕 Cancelar coroutines de auto-refresh
        serviceScope.cancel()

        stopLocationUpdates()
        desconectarWebSocket()

        Log.d(TAG, "🧹 Limpieza completada")
    }
}