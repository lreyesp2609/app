package com.example.app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.app.BuildConfig
import com.example.app.utils.SessionManager
import com.example.app.websocket.WebSocketLocationManager
import com.google.android.gms.location.*
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject


class LocationTrackingService : Service() {

    companion object {
        private const val TAG = "📍LocationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val UPDATE_INTERVAL = 5000L // 5 segundos
        private const val FASTEST_INTERVAL = 3000L // 3 segundos

        const val ACTION_START_TRACKING = "START_TRACKING"
        const val ACTION_STOP_TRACKING = "STOP_TRACKING"
        const val EXTRA_GRUPO_ID = "grupo_id"
        const val EXTRA_GRUPO_NOMBRE = "grupo_nombre"

        /**
         * Iniciar rastreo en segundo plano
         */
        fun startTracking(context: Context, grupoId: Int, grupoNombre: String) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_GRUPO_ID, grupoId)
                putExtra(EXTRA_GRUPO_NOMBRE, grupoNombre)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Detener rastreo
         */
        fun stopTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }

        /**
         * Verificar si está activo
         */
        fun isTracking(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (LocationTrackingService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sessionManager: SessionManager

    private var grupoId: Int = -1
    private var grupoNombre: String = ""
    private var isWebSocketReady = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🎬 Servicio de ubicación creado")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager.getInstance(this)

        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                grupoId = intent.getIntExtra(EXTRA_GRUPO_ID, -1)
                grupoNombre = intent.getStringExtra(EXTRA_GRUPO_NOMBRE) ?: "Grupo"

                if (grupoId != -1) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    ensureWebSocketConnected()
                    startLocationUpdates()
                    Log.d(TAG, "✅ Rastreo iniciado para grupo $grupoId")
                } else {
                    Log.e(TAG, "❌ Grupo ID inválido")
                    stopSelf()
                }
            }
            ACTION_STOP_TRACKING -> {
                stopTracking()
            }
        }

        return START_STICKY // El sistema reiniciará el servicio si lo mata
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Asegurar que WebSocket esté conectado
     * Usa el WebSocketLocationManager existente
     */
    private fun ensureWebSocketConnected() {
        val token = sessionManager.getAccessToken()
        if (token == null) {
            Log.e(TAG, "❌ No hay token disponible")
            stopSelf()
            return
        }

        // Verificar si ya está conectado
        if (WebSocketLocationManager.isConnected()) {
            Log.d(TAG, "✅ WebSocket ya conectado, reutilizando")
            isWebSocketReady = true
            updateNotification("Compartiendo ubicación en $grupoNombre")
            return
        }

        // Conectar WebSocket
        val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")
        val wsUrl = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") +
                "/grupos/ws/ubicaciones?grupo_id=$grupoId&token=$token"

        Log.d(TAG, "🔌 Conectando WebSocket desde servicio...")
        Log.d(TAG, "   Grupo: $grupoId")
        Log.d(TAG, "   URL: ${wsUrl.substringBefore("?")}")

        val serviceListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isWebSocketReady = true
                Log.d(TAG, "✅ WebSocket conectado desde servicio")
                updateNotification("Compartiendo ubicación en $grupoNombre")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isWebSocketReady = false
                Log.e(TAG, "❌ Error en WebSocket: ${t.message}")
                updateNotification("Error de conexión - Reintentando...")

                // Reintentar en 10 segundos
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    if (grupoId != -1) {
                        ensureWebSocketConnected()
                    }
                }, 10000)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isWebSocketReady = false
                Log.d(TAG, "🔒 WebSocket cerrado: $code - $reason")

                // Si el servicio aún está activo, reconectar
                if (grupoId != -1) {
                    android.os.Handler(Looper.getMainLooper()).postDelayed({
                        ensureWebSocketConnected()
                    }, 5000)
                }
            }
        }

        // Usar el manager existente
        WebSocketLocationManager.connect(wsUrl, serviceListener)
    }

    /**
     * Configurar callback de ubicación GPS
     */
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    sendLocationUpdate(location)
                }
            }
        }
    }

    /**
     * Iniciar actualizaciones GPS
     */
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = UPDATE_INTERVAL
            fastestInterval = FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "✅ Actualizaciones GPS iniciadas (cada ${UPDATE_INTERVAL/1000}s)")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Sin permisos de ubicación: ${e.message}")
            stopSelf()
        }
    }

    /**
     * Enviar ubicación al WebSocket
     * Usa el WebSocketLocationManager.send() existente
     */
    private fun sendLocationUpdate(location: Location) {
        if (!isWebSocketReady) {
            Log.w(TAG, "⚠️ WebSocket no listo, reintentando conexión...")
            ensureWebSocketConnected()
            return
        }

        val message = JSONObject().apply {
            put("type", "ubicacion")
            put("lat", location.latitude)
            put("lon", location.longitude)
        }.toString()

        // Usar el manager existente para enviar
        val sent = WebSocketLocationManager.send(message)

        if (sent) {
            Log.v(TAG, "📤 Ubicación enviada: ${location.latitude}, ${location.longitude}")
        } else {
            Log.w(TAG, "⚠️ Error al enviar ubicación")
            isWebSocketReady = false
        }
    }

    /**
     * Detener rastreo
     */
    private fun stopTracking() {
        Log.d(TAG, "🛑 Deteniendo rastreo...")

        // Detener GPS
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // NO cerrar el WebSocket aquí - puede estar siendo usado por otros componentes
        // Solo resetear estado
        grupoId = -1
        isWebSocketReady = false

        stopForeground(true)
        stopSelf()

        Log.d(TAG, "✅ Servicio detenido")
    }

    /**
     * Crear canal de notificación (Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreo de Ubicación",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente mientras se comparte ubicación"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Crear notificación inicial
     */
    private fun createNotification(): Notification {
        // Intent para abrir la app
        val notificationIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Intent para detener el servicio
        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Compartiendo ubicación")
            .setContentText("Conectando a $grupoNombre...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Ícono temporal
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Detener",
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Actualizar texto de la notificación
     */
    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Compartiendo ubicación")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "🧹 Servicio destruido")
    }
}