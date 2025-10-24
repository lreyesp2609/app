package com.example.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.app.BuildConfig
import com.example.app.R
import com.example.app.network.WebSocketLocationManager
import com.example.app.utils.SessionManager
import com.google.android.gms.location.*
import com.google.gson.Gson
import okhttp3.*
import java.util.concurrent.TimeUnit

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentGrupoId: Int = -1

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
        Log.d(TAG, "🎬 LocationService creado")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    enviarUbicacion(location)
                }
            }
        }
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

                Log.d(TAG, "▶️ Iniciando servicio de ubicación para grupo $currentGrupoId")
                startForeground(NOTIFICATION_ID, createNotification())
                conectarWebSocket()
                startLocationUpdates()
            }
            ACTION_STOP -> {
                Log.d(TAG, "⏹️ Deteniendo servicio de ubicación")
                stopLocationUpdates()
                desconectarWebSocket()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun conectarWebSocket() {
        val sessionManager = SessionManager.getInstance(this)
        val token = sessionManager.getAccessToken()

        if (token == null) {
            Log.e(TAG, "❌ No hay token disponible")
            return
        }

        // ✅ USAR EL MANAGER en lugar de crear WebSocket propio
        if (WebSocketLocationManager.isConnected()) {
            Log.d(TAG, "✅ WebSocket ya está conectado")
            return
        }

        val url = obtenerWebSocketUrl(currentGrupoId, token)
        Log.d(TAG, "🔌 Conectando WebSocket desde servicio usando Manager")
        Log.d(TAG, "   URL: $url")

        // ✅ Conectar usando el Manager compartido
        WebSocketLocationManager.connect(url, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "✅ WebSocket conectado desde servicio")
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
                Log.e(TAG, "❌ Error en WebSocket: ${t.message}")

                // Reintentar conexión después de 5 segundos
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    if (currentGrupoId != -1) {
                        Log.d(TAG, "🔄 Reintentando conexión WebSocket...")
                        conectarWebSocket()
                    }
                }, 5000)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "🔒 WebSocket cerrado: $code - $reason")
            }
        })
    }

    private fun desconectarWebSocket() {
        Log.d(TAG, "🔒 Desconectando WebSocket")
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
            Log.d(TAG, "✅ Actualizaciones de ubicación iniciadas")
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
        stopLocationUpdates()
        desconectarWebSocket()
        Log.d(TAG, "🧹 LocationService destruido")
    }
}