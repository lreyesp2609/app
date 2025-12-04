package com.example.app.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
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

class LocationReminderService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: ReminderRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeGeofences = mutableSetOf<Int>()

    // 🔥 NUEVO: WakeLock para mantener el servicio activo
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val NOTIFICATION_ID = 12345
        private const val CHANNEL_ID = "location_reminder_service"
        private const val CHANNEL_NAME = "Servicio de Recordatorios por Ubicación"

        fun start(context: Context) {
            val intent = Intent(context, LocationReminderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationReminderService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("LocationService", "🚀 Servicio de ubicación creado")

        // 🔥 NUEVO: Adquirir WakeLock
        acquireWakeLock()

        val database = AppDatabase.getDatabase(applicationContext)
        repository = ReminderRepository(database.reminderDao())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        setupLocationCallback()
        startLocationUpdates()
    }

    // 🔥 NUEVO: Adquirir WakeLock para mantener el servicio activo
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RecuerdaGo::LocationServiceWakeLock"
        )
        wakeLock?.acquire()
        Log.d("LocationService", "🔋 WakeLock adquirido")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 🔥 CAMBIO: DEFAULT en lugar de LOW para el servicio
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT // 🔥 Cambiado de LOW a DEFAULT
            ).apply {
                description = "Rastrea tu ubicación para recordatorios basados en geolocalización"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recordatorios activos")
            .setContentText("Rastreando ubicación para recordatorios cercanos")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Esto está bien para el servicio
            .setOngoing(true)
            .build()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d("LocationService", "📍 Ubicación recibida: ${location.latitude}, ${location.longitude}")
                    handleLocationUpdate(location.latitude, location.longitude)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            Log.e("LocationService", "❌ Sin permisos de ubicación")
            stopSelf()
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // Cada 10 segundos
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
            Log.d("LocationService", "✅ Actualizaciones de ubicación iniciadas")
        } catch (e: SecurityException) {
            Log.e("LocationService", "❌ SecurityException: ${e.message}")
            stopSelf()
        } catch (e: Exception) {
            Log.e("LocationService", "❌ Error: ${e.message}")
            stopSelf()
        }
    }

    private fun handleLocationUpdate(lat: Double, lon: Double) {
        serviceScope.launch {
            try {
                Log.d("LocationService", "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d("LocationService", "📍 TU UBICACIÓN ACTUAL: $lat, $lon")

                val allReminders = repository.getAllRemindersForLocationService()
                val activeReminders = allReminders.filter {
                    (it.reminder_type == "location" || it.reminder_type == "both") &&
                            it.latitude != null && it.longitude != null &&
                            it.is_active == true && it.is_deleted == false
                }

                Log.d("LocationService", "✅ Recordatorios ACTIVOS: ${activeReminders.size}")

                if (activeReminders.isEmpty()) {
                    Log.w("LocationService", "⚠️ NO HAY RECORDATORIOS ACTIVOS")
                    return@launch
                }

                for (reminder in activeReminders) {
                    try {
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
                                    Log.d("LocationService", "   🔔 DISPARANDO: ENTRADA")
                                    triggerLocationNotification(reminder, "Entraste en la zona")
                                }
                            }
                            !inside && wasInside -> {
                                activeGeofences.remove(reminder.id)
                                if (reminder.trigger_type == "exit" || reminder.trigger_type == "both") {
                                    Log.d("LocationService", "   🔔 DISPARANDO: SALIDA")
                                    triggerLocationNotification(reminder, "Saliste de la zona")
                                }
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("LocationService", "❌ Error ID ${reminder.id}: ${e.message}")
                    }
                }

                Log.d("LocationService", "━━━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                Log.e("LocationService", "❌ ERROR CRÍTICO: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun triggerLocationNotification(reminder: ReminderEntity, transition: String) {
        Log.d("LocationService", "🔔 CREANDO NOTIFICACIÓN:")

        // 🔥 DESPERTAR DISPOSITIVO
        NotificationHelper.wakeUpDevice(applicationContext)

        NotificationHelper.createNotificationChannel(applicationContext)

        // 🔥 Intent para pantalla completa
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reminder_id", reminder.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            reminder.id,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(reminder.title)
            .setContentText("${reminder.description ?: "Sin descripción"} ($transition)")
            .setPriority(NotificationCompat.PRIORITY_MAX) // 🔥 PRIORITY_MAX
            .setCategory(NotificationCompat.CATEGORY_ALARM) // 🔥 CATEGORY_ALARM
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true) // 🔥 Pantalla completa
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // 🔥 DEFAULT_ALL

        if (reminder.sound) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            Log.d("LocationService", "   🔊 Sonido activado")
        }

        if (reminder.vibration) {
            builder.setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            Log.d("LocationService", "   📳 Vibración activada")
        }

        val notificationId = Random.nextInt(1000, 9999)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())

        Log.d("LocationService", "   ✅ Notificación enviada exitosamente")
    }

    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val start = android.location.Location("start").apply {
            latitude = lat1
            longitude = lon1
        }
        val end = android.location.Location("end").apply {
            latitude = lat2
            longitude = lon2
        }
        return start.distanceTo(end)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LocationService", "🛑 Servicio destruido")

        // 🔥 LIBERAR WAKELOCK
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d("LocationService", "🔋 WakeLock liberado")
            }
        }

        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
