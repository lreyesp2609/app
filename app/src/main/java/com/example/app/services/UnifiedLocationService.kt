package com.example.app.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.app.MainActivity
import com.example.app.models.ReminderEntity
import com.example.app.network.AppDatabase
import com.example.app.utils.NotificationHelper
import com.example.app.repository.ReminderRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlin.random.Random
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.location.Location
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Handler
import com.example.app.network.RetrofitClient
import com.example.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import com.example.app.network.VerificarUbicacionRequest
import com.example.app.network.VerificarUbicacionResponse
import com.example.app.network.ZonaPeligrosaDetectada

class UnifiedLocationService : Service() {

    // ════════════════════════════════════════
    // COMPONENTES DEL SERVICIO
    // ════════════════════════════════════════

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var sessionManager: SessionManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeGeofences = mutableSetOf<Int>()
    private var wakeLock: PowerManager.WakeLock? = null

    // Para batch tracking
    private val puntosGPSAcumulados = mutableListOf<PuntoGPSBatch>()
    private val handler = Handler(Looper.getMainLooper())
    private val INTERVALO_BATCH = 120_000L // 2 minutos


    // ════════════════════════════════════════
    // 🆕 NUEVO: Variables para Zonas Peligrosas
    // ════════════════════════════════════════
    private val zonasActivasUsuario = mutableSetOf<Int>() // Zonas donde está AHORA
    private val ultimaVerificacionZona = mutableMapOf<Int, Long>() // Evitar spam
    private val INTERVALO_MIN_ALERTA = 300_000L   // 5 minutos entre alertas de la misma zona


    companion object {
        private const val NOTIFICATION_ID = 12345
        private const val CHANNEL_ID = "unified_location_service"
        private const val TAG = "UnifiedLocation"

        fun start(context: Context) {
            val intent = Intent(context, UnifiedLocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, UnifiedLocationService::class.java)
            context.stopService(intent)
        }
    }

    // ════════════════════════════════════════
    // LIFECYCLE
    // ════════════════════════════════════════

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Servicio unificado creado")

        acquireWakeLock()

        // Inicializar componentes
        val database = AppDatabase.getDatabase(applicationContext)
        reminderRepository = ReminderRepository(database.reminderDao())
        sessionManager = SessionManager.getInstance(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar notificación y ubicación
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        setupLocationCallback()
        startLocationUpdates()

        // Iniciar envío en lote
        iniciarEnvioBatch()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RecuerdaGo::UnifiedLocationWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutos
        Log.d(TAG, "🔋 WakeLock adquirido")
    }

    // ════════════════════════════════════════
    // NOTIFICACIÓN
    // ════════════════════════════════════════

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Ubicación",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitorea recordatorios y analiza rutas"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("RecuerdaGo activo")
            .setContentText("Monitoreando ubicación")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSound(null)
            .setVibrate(null)
            .build()
    }

    // ════════════════════════════════════════
    // UBICACIÓN
    // ════════════════════════════════════════

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d(TAG, "📍 Ubicación: ${location.latitude}, ${location.longitude}")

                    // 1️⃣ Verificar recordatorios (tu código existente)
                    handleReminderGeofencing(location.latitude, location.longitude)

                    // 2️⃣ 🆕 NUEVO: Verificar zonas peligrosas del usuario
                    verificarZonasPeligrosas(location.latitude, location.longitude)

                    // 3️⃣ Guardar para batch tracking
                    guardarPuntoParaBatch(location)
                }
            }
        }
    }

    private fun verificarZonasPeligrosas(lat: Double, lon: Double) {
        serviceScope.launch {
            try {
                val token = sessionManager.getAccessToken() ?: run {
                    Log.e(TAG, "❌ No hay token de acceso")
                    return@launch
                }

                Log.d(TAG, "🔍 Verificando zonas peligrosas en ($lat, $lon)")

                val request = VerificarUbicacionRequest(lat, lon)
                val response = RetrofitClient.seguridadApiService.verificarUbicacionActual(
                    token = "Bearer $token",
                    request = request
                )

                Log.d(TAG, "📥 Respuesta recibida:")
                Log.d(TAG, "   hay_peligro: ${response.hay_peligro}")
                Log.d(TAG, "   zonas_detectadas: ${response.zonas_detectadas.size}")
                Log.d(TAG, "   mensaje_alerta: ${response.mensaje_alerta}")

                if (response.hay_peligro) {
                    Log.w(TAG, "🚨 ¡PELIGRO DETECTADO! Procesando alerta...")
                    manejarAlertaZonaPeligrosa(response)
                } else {
                    if (zonasActivasUsuario.isNotEmpty()) {
                        Log.d(TAG, "✅ Usuario salió de zonas peligrosas")
                        zonasActivasUsuario.clear()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error verificando zonas peligrosas: ${e.message}")
                e.printStackTrace() // 🔥 Esto mostrará el stacktrace completo
            }
        }
    }

    private fun manejarAlertaZonaPeligrosa(response: VerificarUbicacionResponse) {
        Log.d(TAG, "🎯 manejarAlertaZonaPeligrosa EJECUTADO")
        Log.d(TAG, "   Zonas detectadas: ${response.zonas_detectadas.size}")

        val ahora = System.currentTimeMillis()

        response.zonas_detectadas.forEach { zona ->
            Log.d(TAG, "   Procesando zona: ${zona.nombre} (ID: ${zona.zona_id})")

            val yaEstabaAdentro = zonasActivasUsuario.contains(zona.zona_id)
            Log.d(TAG, "     ¿Ya estaba adentro? $yaEstabaAdentro")

            if (!yaEstabaAdentro) {
                // 🚨 ENTRADA A ZONA PELIGROSA (NUEVA)
                Log.w(TAG, "🚨 ENTRADA NUEVA A ZONA: ${zona.nombre}")
                zonasActivasUsuario.add(zona.zona_id)

                // Verificar cooldown
                val ultimaAlerta = ultimaVerificacionZona[zona.zona_id] ?: 0L
                val tiempoTranscurrido = ahora - ultimaAlerta

                Log.d(TAG, "     Última alerta hace: ${tiempoTranscurrido / 1000}s")
                Log.d(TAG, "     Cooldown necesario: ${INTERVALO_MIN_ALERTA / 1000}s")

                if (tiempoTranscurrido >= INTERVALO_MIN_ALERTA) {
                    Log.w(TAG, "✅ Mostrando alerta (cooldown pasado)")
                    mostrarAlertaZonaPeligrosa(zona, response.mensaje_alerta)
                    ultimaVerificacionZona[zona.zona_id] = ahora
                } else {
                    Log.w(TAG, "⏳ Cooldown activo, faltan ${(INTERVALO_MIN_ALERTA - tiempoTranscurrido) / 1000}s")
                }
            } else {
                // 🔄 USUARIO SIGUE DENTRO
                Log.d(TAG, "🔄 Usuario continúa dentro de: ${zona.nombre}")
            }
        }

        // Limpiar zonas de las que salió
        val zonasActuales = response.zonas_detectadas.map { it.zona_id }.toSet()
        val zonasEliminadas = zonasActivasUsuario - zonasActuales

        if (zonasEliminadas.isNotEmpty()) {
            Log.d(TAG, "🚪 Usuario salió de zonas: $zonasEliminadas")
            zonasActivasUsuario.retainAll(zonasActuales)
        }
    }

    private fun mostrarAlertaZonaPeligrosa(zona: ZonaPeligrosaDetectada, mensajeAlerta: String?) {
        Log.w(TAG, "🔔 mostrarAlertaZonaPeligrosa() EJECUTADO")
        Log.w(TAG, "   Zona: ${zona.nombre}")
        Log.w(TAG, "   Nivel: ${zona.nivel_peligro}")

        // 🔊 REPRODUCIR SONIDO MANUALMENTE (ANTES de crear la notificación)
        reproducirSonidoAlarma()

        // Despertar dispositivo
        NotificationHelper.wakeUpDevice(applicationContext)

        val titulo = when (zona.nivel_peligro) {
            5 -> "🚨 ZONA MUY PELIGROSA"
            4 -> "⚠️ ZONA PELIGROSA"
            3 -> "⚠️ ZONA DE RIESGO"
            else -> "ℹ️ Zona Marcada"
        }

        val descripcion = mensajeAlerta ?: zona.nombre

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_ZONA_PELIGRO", zona.zona_id)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, zona.zona_id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = NotificationHelper.createAlertChannel(applicationContext)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titulo)
            .setContentText(descripcion)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$descripcion\n\nDistancia: ${zona.distancia_al_centro.toInt()}m\n" +
                        "Nivel de peligro: ${zona.nivel_peligro}/5"
            ))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        val notificationId = 10000 + zona.zona_id
        notificationManager.notify(notificationId, notification)

        Log.w(TAG, "✅ Notificación #$notificationId mostrada")
        Log.w(TAG, "🚨 ALERTA: ${zona.nombre} (Nivel ${zona.nivel_peligro})")
    }

    // 🆕 NUEVO MÉTODO: Reproducir sonido manualmente
    private var currentRingtone: Ringtone? = null

    private fun reproducirSonidoAlarma() {
        try {
            // Detener sonido anterior si existe
            currentRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                    Log.d(TAG, "🔇 Sonido anterior detenido")
                }
            }

            // Obtener URI de alarma
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            Log.d(TAG, "🔊 Intentando reproducir: $soundUri")

            // Crear y reproducir ringtone
            val ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)

            if (ringtone != null) {
                // Configurar para reproducir a máximo volumen
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.volume = 1.0f
                }

                ringtone.play()
                currentRingtone = ringtone

                Log.d(TAG, "✅ Sonido reproducido (durará 5 segundos)")

                // Detener después de 5 segundos
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        if (ringtone.isPlaying) {
                            ringtone.stop()
                            Log.d(TAG, "🔇 Sonido detenido automáticamente")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deteniendo sonido: ${e.message}")
                    }
                }, 5000)

            } else {
                Log.e(TAG, "❌ Ringtone es NULL, intentando fallback...")
                reproducirSonidoFallback()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reproduciendo sonido: ${e.message}")
            e.printStackTrace()
            reproducirSonidoFallback()
        }
    }

    // Fallback por si TYPE_ALARM no funciona
    private fun reproducirSonidoFallback() {
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)

            if (ringtone != null) {
                ringtone.play()
                currentRingtone = ringtone
                Log.d(TAG, "✅ Sonido fallback (RINGTONE) reproducido")

                Handler(Looper.getMainLooper()).postDelayed({
                    ringtone.stop()
                }, 5000)
            } else {
                Log.e(TAG, "❌ Fallback también falló")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en fallback: ${e.message}")
        }
    }

    private fun startLocationUpdates() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            Log.e(TAG, "❌ Sin permisos de ubicación")
            stopSelf()
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // 10 segundos
        ).apply {
            setMinUpdateIntervalMillis(5000L)
            setMaxUpdateDelayMillis(15000L)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "✅ Actualizaciones de ubicación iniciadas")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException: ${e.message}")
            stopSelf()
        }
    }

    // ════════════════════════════════════════
    // 1️⃣ RECORDATORIOS (GEOFENCING)
    // ════════════════════════════════════════

    private fun handleReminderGeofencing(lat: Double, lon: Double) {
        serviceScope.launch {
            try {
                val activeReminders = reminderRepository.getAllRemindersForLocationService().filter {
                    (it.reminder_type == "location" || it.reminder_type == "both") &&
                            it.latitude != null && it.longitude != null &&
                            it.is_active == true && it.is_deleted == false
                }

                for (reminder in activeReminders) {
                    val reminderLat = reminder.latitude ?: continue
                    val reminderLon = reminder.longitude ?: continue
                    val distance = calcularDistancia(lat, lon, reminderLat, reminderLon)
                    val radius = reminder.radius ?: 100f

                    val inside = distance <= radius
                    val wasInside = activeGeofences.contains(reminder.id)

                    when {
                        inside && !wasInside -> {
                            activeGeofences.add(reminder.id)
                            if (reminder.trigger_type == "enter" || reminder.trigger_type == "both") {
                                triggerReminderNotification(reminder, "Entraste en la zona")
                            }
                        }
                        !inside && wasInside -> {
                            activeGeofences.remove(reminder.id)
                            if (reminder.trigger_type == "exit" || reminder.trigger_type == "both") {
                                triggerReminderNotification(reminder, "Saliste de la zona")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en geofencing: ${e.message}")
            }
        }
    }

    private fun triggerReminderNotification(reminder: ReminderEntity, transition: String) {
        NotificationHelper.wakeUpDevice(applicationContext)

        val channelId = when {
            reminder.sound && !reminder.sound_uri.isNullOrEmpty() ->
                NotificationHelper.createCustomSoundChannel(applicationContext, reminder.sound_uri)
            reminder.sound -> NotificationHelper.CHANNEL_ID
            else -> NotificationHelper.createSilentChannel(applicationContext)
        }

        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reminder_id", reminder.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, reminder.id, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(reminder.title)
            .setContentText("${reminder.description ?: ""} ($transition)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)

        if (reminder.vibration) {
            builder.setVibrate(longArrayOf(0, 500, 250, 500))
        }

        val notificationId = Random.nextInt(1000, 9999)
        getSystemService(NotificationManager::class.java).notify(notificationId, builder.build())
    }

    // ════════════════════════════════════════
    // 2️⃣ BATCH TRACKING
    // ════════════════════════════════════════

    private fun guardarPuntoParaBatch(location: Location) {
        // ✅ FIX CRÍTICO: location.time YA está en UTC (milisegundos desde epoch)
        // Solo necesitas formatear correctamente
        val timestampUTC = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26+: Usar java.time (recomendado)
            Instant.ofEpochMilli(location.time)
                .toString()  // Ya retorna formato ISO 8601 en UTC
        } else {
            // API < 26: SimpleDateFormat DEBE tener timezone UTC
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC") // ⚠️ SIN ESTO NO FUNCIONA
            }.format(Date(location.time))
        }

        val punto = PuntoGPSBatch(
            lat = location.latitude,
            lon = location.longitude,
            timestamp = timestampUTC, // ✅ Ahora es UTC real
            precision = if (location.hasAccuracy()) location.accuracy else null,
            velocidad = if (location.hasSpeed()) location.speed else null
        )

        // Log para debug (temporal)
        Log.d(TAG, """
        📍 Punto GPS capturado:
           - Timestamp UTC: $timestampUTC
           - Hora del dispositivo: ${Date()}
           - location.time: ${Date(location.time)}
           - Timezone del dispositivo: ${TimeZone.getDefault().id}
    """.trimIndent())

        synchronized(puntosGPSAcumulados) {
            puntosGPSAcumulados.add(punto)
        }

        // Enviar si hay muchos puntos acumulados
        if (puntosGPSAcumulados.size >= 12) {
            enviarPuntosAcumulados()
        }
    }

    private fun iniciarEnvioBatch() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                enviarPuntosAcumulados()
                handler.postDelayed(this, INTERVALO_BATCH)
            }
        }, INTERVALO_BATCH)
    }

    private fun enviarPuntosAcumulados() {
        val puntos: List<PuntoGPSBatch>
        synchronized(puntosGPSAcumulados) {
            if (puntosGPSAcumulados.isEmpty()) return
            puntos = puntosGPSAcumulados.toList()
            puntosGPSAcumulados.clear()
        }

        serviceScope.launch {
            try {
                val token = sessionManager.getAccessToken() ?: return@launch
                val request = LotePuntosGPSRequest(puntos = puntos)
                val response = RetrofitClient.trackingApiService.guardarLotePuntosGPS(
                    token = "Bearer $token",
                    request = request
                )
                Log.d(TAG, "✅ ${response.puntos_guardados} puntos enviados")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error enviando puntos: ${e.message}")
                synchronized(puntosGPSAcumulados) {
                    puntosGPSAcumulados.addAll(0, puntos)
                }
            }
        }
    }

    // ════════════════════════════════════════
    // UTILIDADES
    // ════════════════════════════════════════

    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val start = Location("start").apply { latitude = lat1; longitude = lon1 }
        val end = Location("end").apply { latitude = lat2; longitude = lon2 }
        return start.distanceTo(end)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🛑 Servicio destruido")

        wakeLock?.let {
            if (it.isHeld) it.release()
        }

        handler.removeCallbacksAndMessages(null)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()

        // Enviar puntos restantes
        if (puntosGPSAcumulados.isNotEmpty()) {
            enviarPuntosAcumulados()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

// Data classes (mantener las mismas)
data class PuntoGPSBatch(
    val lat: Double,
    val lon: Double,
    val timestamp: String,
    val precision: Float? = null,
    val velocidad: Float? = null
)

data class LotePuntosGPSRequest(val puntos: List<PuntoGPSBatch>)
data class LotePuntosGPSResponse(
    val success: Boolean,
    val puntos_guardados: Int,
    val message: String
)

data class VerificarUbicacionRequest(
    val lat: Double,
    val lon: Double
)

data class VerificarUbicacionResponse(
    val hay_peligro: Boolean,
    val zonas_detectadas: List<ZonaPeligrosaDetectada>,
    val mensaje_alerta: String?
)

data class ZonaPeligrosaDetectada(
    val zona_id: Int,
    val nombre: String,
    val nivel_peligro: Int,
    val tipo: String?,
    val distancia_al_centro: Float,
    val dentro_de_zona: Boolean
)