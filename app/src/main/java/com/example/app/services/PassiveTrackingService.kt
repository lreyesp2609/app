package com.example.app.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.app.network.RetrofitClient
import com.example.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class PassiveTrackingService : Service(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val handler = Handler(Looper.getMainLooper())
    private val INTERVALO_GPS = 10_000L // ⚡ 10 segundos (más preciso)
    private val INTERVALO_BATCH = 120_000L // 📦 2 minutos para envío en lote

    private lateinit var locationManager: LocationManager
    private lateinit var sessionManager: SessionManager

    private val puntosGPSAcumulados = mutableListOf<PuntoGPSBatch>()
    private var isRunning = false

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "passive_tracking_channel"
        private const val TAG = "PassiveTracking"

        // Acción para detener el servicio
        const val ACTION_STOP_TRACKING = "com.example.app.STOP_TRACKING"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🟢 Servicio de tracking pasivo creado")

        sessionManager = SessionManager.getInstance(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        crearNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_TRACKING) {
            Log.d(TAG, "🛑 Deteniendo tracking por solicitud del usuario")
            detenerTracking()
            return START_NOT_STICKY
        }

        if (!isRunning) {
            isRunning = true
            iniciarTrackingGPS()
            iniciarEnvioBatch()

            // Iniciar como servicio foreground
            startForeground(NOTIFICATION_ID, crearNotificacion())

            Log.d(TAG, "✅ Tracking pasivo iniciado")
        }

        return START_STICKY
    }

    private fun iniciarTrackingGPS() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isRunning) {
                    obtenerUbicacionYGuardar()
                    handler.postDelayed(this, INTERVALO_GPS)
                }
            }
        }, 0) // Empezar inmediatamente
    }

    private fun iniciarEnvioBatch() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isRunning) {
                    enviarPuntosAcumulados()
                    handler.postDelayed(this, INTERVALO_BATCH)
                }
            }
        }, INTERVALO_BATCH)
    }

    private fun obtenerUbicacionYGuardar() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "⚠️ Sin permisos de ubicación")
            return
        }

        try {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            location?.let {
                guardarPuntoLocal(it)
            } ?: run {
                Log.w(TAG, "⚠️ No se pudo obtener ubicación")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo ubicación: ${e.message}")
        }
    }

    private fun guardarPuntoLocal(location: Location) {
        val punto = PuntoGPSBatch(
            lat = location.latitude,
            lon = location.longitude,
            timestamp = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                Locale.US
            ).format(Date(location.time)),
            precision = if (location.hasAccuracy()) location.accuracy else null,
            velocidad = if (location.hasSpeed()) location.speed else null
        )

        synchronized(puntosGPSAcumulados) {
            puntosGPSAcumulados.add(punto)

            Log.d(TAG, "📍 Punto GPS guardado localmente: (${location.latitude}, ${location.longitude})")
            Log.d(TAG, "   Total acumulados: ${puntosGPSAcumulados.size}")
        }

        // Si acumulamos muchos puntos, enviar inmediatamente
        if (puntosGPSAcumulados.size >= 12) { // ⚡ 12 puntos = 2 minutos de tracking
            enviarPuntosAcumulados()
        }
    }

    private fun enviarPuntosAcumulados() {
        val puntos: List<PuntoGPSBatch>

        synchronized(puntosGPSAcumulados) {
            if (puntosGPSAcumulados.isEmpty()) {
                return
            }

            puntos = puntosGPSAcumulados.toList()
            puntosGPSAcumulados.clear()
        }

        Log.d(TAG, "📤 Enviando ${puntos.size} puntos GPS al backend...")

        launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    sessionManager.getAccessToken()
                }

                if (token.isNullOrEmpty()) {
                    Log.w(TAG, "⚠️ Sin token de autenticación, acumulando puntos...")
                    synchronized(puntosGPSAcumulados) {
                        puntosGPSAcumulados.addAll(0, puntos)
                    }
                    return@launch
                }

                val request = LotePuntosGPSRequest(puntos = puntos)

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.trackingApiService.guardarLotePuntosGPS(
                        token = "Bearer $token",
                        request = request
                    )
                }

                Log.d(TAG, "✅ ${response.puntos_guardados} puntos enviados exitosamente")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error enviando puntos GPS: ${e.message}")

                // Volver a acumular para reintento
                synchronized(puntosGPSAcumulados) {
                    puntosGPSAcumulados.addAll(0, puntos)

                    // Limitar a 100 puntos máximo en memoria
                    if (puntosGPSAcumulados.size > 100) {
                        puntosGPSAcumulados.subList(100, puntosGPSAcumulados.size).clear()
                    }
                }
            }
        }
    }

    private fun crearNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tracking GPS Pasivo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoreo de ubicación para detectar patrones"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun crearNotificacion(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RecuerdaGo - Tracking activo")
            .setContentText("Analizando tus rutas para mayor seguridad")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // ✅ Icono por defecto de Android
            .setPriority(NotificationCompat.PRIORITY_LOW) // ✅ Corregido
            .setOngoing(true)
            .build()
    }

    private fun detenerTracking() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)

        // Enviar puntos restantes
        if (puntosGPSAcumulados.isNotEmpty()) {
            enviarPuntosAcumulados()
        }

        stopForeground(true)
        stopSelf()

        Log.d(TAG, "🛑 Tracking pasivo detenido")
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerTracking()
        job.cancel()
        Log.d(TAG, "🔴 Servicio de tracking destruido")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

// ══════════════════════════════════════════════════════════
// DATA CLASSES
// ══════════════════════════════════════════════════════════

data class PuntoGPSBatch(
    val lat: Double,
    val lon: Double,
    val timestamp: String,
    val precision: Float? = null,
    val velocidad: Float? = null
)

data class LotePuntosGPSRequest(
    val puntos: List<PuntoGPSBatch>
)

data class LotePuntosGPSResponse(
    val success: Boolean,
    val puntos_guardados: Int,
    val message: String
)