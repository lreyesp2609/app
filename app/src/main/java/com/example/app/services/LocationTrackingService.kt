package com.example.app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.app.BuildConfig
import com.example.app.utils.SessionManager
import com.google.android.gms.location.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LocationTrackingService : Service() {

    companion object {
        private const val TAG = "📍LocationTrackingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val UPDATE_INTERVAL = 5000L
        private const val FASTEST_INTERVAL = 3000L

        const val ACTION_START_TRACKING = "START_TRACKING"
        const val ACTION_STOP_TRACKING = "STOP_TRACKING"
        const val ACTION_STOP_ALL = "STOP_ALL"
        const val EXTRA_GRUPO_ID = "grupo_id"
        const val EXTRA_GRUPO_NOMBRE = "grupo_nombre"

        // 🆕 Rastrear grupos activos
        private val activeGroups = mutableSetOf<Int>()

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
         * Detener rastreo de un grupo específico
         */
        fun stopTracking(context: Context, grupoId: Int) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
                putExtra(EXTRA_GRUPO_ID, grupoId)
            }
            context.startService(intent)
        }

        /**
         * Detener rastreo de TODOS los grupos
         */
        fun stopAllTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_ALL
            }
            context.startService(intent)
        }

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

        private var instance: LocationTrackingService? = null

        private val listenersByGroup = mutableMapOf<Int, MutableList<(String) -> Unit>>()

        fun addMessageListener(grupoId: Int, listener: (String) -> Unit) {
            if (!listenersByGroup.containsKey(grupoId)) {
                listenersByGroup[grupoId] = mutableListOf()
            }
            listenersByGroup[grupoId]?.add(listener)
            Log.d(TAG, "📢 Listener agregado para grupo $grupoId. Total: ${listenersByGroup[grupoId]?.size ?: 0}")
        }

        fun removeMessageListener(grupoId: Int, listener: (String) -> Unit) {
            listenersByGroup[grupoId]?.remove(listener)
            if (listenersByGroup[grupoId]?.isEmpty() == true) {
                listenersByGroup.remove(grupoId)
            }
            Log.d(TAG, "📢 Listener removido del grupo $grupoId. Grupos restantes: ${listenersByGroup.size}")
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sessionManager: SessionManager

    // 🆕 Mapa de WebSockets por grupo
    private val webSocketsByGroup = mutableMapOf<Int, WebSocket>()
    private val grupoNombres = mutableMapOf<Int, String>()
    private var locationUpdatesStarted = false

    // 🆕 Listeners locales para broadcast a ViewModels
    private val messageListeners = mutableListOf<(String) -> Unit>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🎬 Servicio de ubicación creado")

        instance = this // 🆕 Guardar instancia

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager.getInstance(this)

        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                val grupoId = intent.getIntExtra(EXTRA_GRUPO_ID, -1)
                val grupoNombre = intent.getStringExtra(EXTRA_GRUPO_NOMBRE) ?: "Grupo $grupoId"

                if (grupoId != -1) {
                    addGrupo(grupoId, grupoNombre)
                } else {
                    Log.e(TAG, "❌ Grupo ID inválido")
                }
            }
            ACTION_STOP_TRACKING -> {
                val grupoId = intent.getIntExtra(EXTRA_GRUPO_ID, -1)
                if (grupoId != -1) {
                    removeGrupo(grupoId)
                }
            }
            ACTION_STOP_ALL -> {
                stopAllGroups()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 🆕 Agregar grupo al rastreo
     */
    private fun addGrupo(grupoId: Int, grupoNombre: String) {
        if (activeGroups.contains(grupoId)) {
            Log.d(TAG, "⚠️ Grupo $grupoId ya está siendo rastreado")
            return
        }

        Log.d(TAG, "➕ ════════════════════════════════════════")
        Log.d(TAG, "➕ AGREGANDO GRUPO AL RASTREO")
        Log.d(TAG, "➕ Grupo: $grupoId - $grupoNombre")
        Log.d(TAG, "➕ ════════════════════════════════════════")

        activeGroups.add(grupoId)
        grupoNombres[grupoId] = grupoNombre

        // Iniciar foreground si es el primer grupo
        if (activeGroups.size == 1) {
            startForeground(NOTIFICATION_ID, createNotification())
        } else {
            updateNotification()
        }

        // Conectar WebSocket para este grupo
        connectWebSocketForGroup(grupoId, grupoNombre)

        // Iniciar GPS si no estaba iniciado
        if (!locationUpdatesStarted) {
            startLocationUpdates()
        }
    }

    /**
     * 🆕 Remover grupo del rastreo
     */
    private fun removeGrupo(grupoId: Int) {
        if (!activeGroups.contains(grupoId)) {
            Log.d(TAG, "⚠️ Grupo $grupoId no estaba siendo rastreado")
            return
        }

        Log.d(TAG, "➖ ════════════════════════════════════════")
        Log.d(TAG, "➖ REMOVIENDO GRUPO DEL RASTREO")
        Log.d(TAG, "➖ Grupo: $grupoId")
        Log.d(TAG, "➖ ════════════════════════════════════════")

        activeGroups.remove(grupoId)
        grupoNombres.remove(grupoId)

        // Cerrar WebSocket de este grupo
        webSocketsByGroup[grupoId]?.close(1000, "Usuario dejó el grupo")
        webSocketsByGroup.remove(grupoId)

        // Si no quedan grupos, detener servicio
        if (activeGroups.isEmpty()) {
            Log.d(TAG, "🛑 No quedan grupos activos, deteniendo servicio")
            stopTracking()
        } else {
            updateNotification()
        }
    }

    /**
     * 🆕 Detener todos los grupos
     */
    private fun stopAllGroups() {
        Log.d(TAG, "🛑 Deteniendo rastreo de todos los grupos")

        activeGroups.toList().forEach { grupoId ->
            removeGrupo(grupoId)
        }

        stopTracking()
    }

    /**
     * 🆕 Conectar WebSocket para un grupo específico
     */
    private fun connectWebSocketForGroup(grupoId: Int, grupoNombre: String) {
        // ✅ Verificar si ya existe conexión para este grupo
        if (webSocketsByGroup.containsKey(grupoId)) {
            Log.d(TAG, "⚠️ Ya existe WebSocket para grupo $grupoId, cerrando anterior")
            webSocketsByGroup[grupoId]?.close(1000, "Reconectando")
            webSocketsByGroup.remove(grupoId)
        }

        val token = sessionManager.getAccessToken()
        if (token == null) {
            Log.e(TAG, "❌ No hay token disponible para grupo $grupoId")
            return
        }

        val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")
        val wsUrl = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") +
                "/ws/$grupoId/ubicaciones?token=$token"

        Log.d(TAG, "🔌 Conectando WebSocket para grupo $grupoId")

        val client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(wsUrl).build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "✅ WebSocket conectado para grupo $grupoId")
                updateNotification()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.v(TAG, "📨 Mensaje recibido del grupo $grupoId: ${text.take(100)}")

                // ✅ Solo notificar a listeners de ESTE grupo específico
                listenersByGroup[grupoId]?.forEach { listener ->
                    try {
                        listener(text)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error en listener del grupo $grupoId: ${e.message}")
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "❌ Error WebSocket grupo $grupoId: ${t.message}")
                Log.e(TAG, "   Respuesta: ${response?.code} ${response?.message}")

                if (response?.code == 403) {
                    Log.w(TAG, "⚠️ 403 Forbidden - Usuario ya conectado o sin permisos")
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (activeGroups.contains(grupoId)) {
                            Log.d(TAG, "🔄 Reintentando conexión después de 403...")
                            connectWebSocketForGroup(grupoId, grupoNombre)
                        }
                    }, 30000)
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (activeGroups.contains(grupoId)) {
                            connectWebSocketForGroup(grupoId, grupoNombre)
                        }
                    }, 10000)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "🔒 WebSocket cerrado para grupo $grupoId: $code - $reason")

                if (activeGroups.contains(grupoId)) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        connectWebSocketForGroup(grupoId, grupoNombre)
                    }, 5000)
                }
            }
        }

        val webSocket = client.newWebSocket(request, listener)
        webSocketsByGroup[grupoId] = webSocket
    }

    /**
     * Configurar callback GPS
     */
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    sendLocationToAllGroups(location)
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
            locationUpdatesStarted = true
            Log.d(TAG, "✅ Actualizaciones GPS iniciadas")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Sin permisos de ubicación: ${e.message}")
        }
    }

    /**
     * 🆕 Enviar ubicación a TODOS los grupos activos
     */
    private fun sendLocationToAllGroups(location: Location) {
        val message = JSONObject().apply {
            put("type", "ubicacion")
            put("lat", location.latitude)
            put("lon", location.longitude)
        }.toString()

        var successCount = 0
        activeGroups.forEach { grupoId ->
            val webSocket = webSocketsByGroup[grupoId]
            if (webSocket != null) {
                val sent = webSocket.send(message)
                if (sent) successCount++
            }
        }

        Log.v(TAG, "📤 Ubicación enviada a $successCount/${activeGroups.size} grupos")
    }

    /**
     * Detener rastreo completamente
     */
    private fun stopTracking() {
        Log.d(TAG, "🛑 Deteniendo servicio completo...")

        // Detener GPS
        if (locationUpdatesStarted) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            locationUpdatesStarted = false
        }

        // Cerrar todos los WebSockets
        webSocketsByGroup.values.forEach { it.close(1000, "Servicio detenido") }
        webSocketsByGroup.clear()

        activeGroups.clear()
        grupoNombres.clear()

        stopForeground(true)
        stopSelf()

        Log.d(TAG, "✅ Servicio detenido completamente")
    }

    /**
     * 🆕 Crear notificación con múltiples grupos
     */
    private fun createNotification(): Notification {
        val text = when (activeGroups.size) {
            0 -> "Iniciando..."
            1 -> {
                val grupoId = activeGroups.first()
                "Compartiendo en ${grupoNombres[grupoId]}"
            }
            else -> "Compartiendo en ${activeGroups.size} grupos"
        }

        val notificationIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP_ALL
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Compartiendo ubicación")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Detener todo",
                stopPendingIntent
            )
            .setOngoing(true)
            .setAutoCancel(false) // 🆕 Evitar que se elimine
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * 🆕 Actualizar notificación existente
     */
    private fun updateNotification() {
        val notification = createNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Canal de notificación
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreo de Ubicación",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Compartiendo ubicación en grupos"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Si hay grupos activos, significa que fue destruido inesperadamente
        if (activeGroups.isNotEmpty()) {
            Log.w(TAG, "⚠️ Servicio destruido inesperadamente con ${activeGroups.size} grupos activos")

            // Guardar grupos para reiniciar
            val gruposToRestart = activeGroups.toList()
            val nombresBackup = grupoNombres.toMap()

            // Reiniciar servicio
            Handler(Looper.getMainLooper()).postDelayed({
                gruposToRestart.forEach { grupoId ->
                    val nombre = nombresBackup[grupoId] ?: "Grupo $grupoId"
                    startTracking(applicationContext, grupoId, nombre)
                }
            }, 2000)
        }

        if (locationUpdatesStarted) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        Log.d(TAG, "🧹 Servicio destruido")
    }
}