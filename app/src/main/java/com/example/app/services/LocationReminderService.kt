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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.app.models.ReminderEntity
import com.example.app.network.AppDatabase
import com.example.app.utils.NotificationHelper
import com.example.app.viewmodel.ReminderRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlin.random.Random

class LocationReminderService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: ReminderRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeGeofences = mutableSetOf<Int>()

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

        // Inicializar repositorio
        val database = AppDatabase.getDatabase(applicationContext)
        repository = ReminderRepository(database.reminderDao())

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Crear canal de notificación
        createNotificationChannel()

        // Iniciar en primer plano
        startForeground(NOTIFICATION_ID, createForegroundNotification())

        // Configurar callback de ubicación
        setupLocationCallback()

        // Iniciar actualizaciones de ubicación
        startLocationUpdates()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
            Priority.PRIORITY_HIGH_ACCURACY, // Cambiado a HIGH para mayor precisión
            10000L // Cada 10 segundos para testing
        ).apply {
            setMinUpdateIntervalMillis(5000L) // Mínimo 5 segundos
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

                // ✅ CORRECCIÓN: Obtener TODOS (incluidos inactivos)
                val allReminders = repository.getAllRemindersForLocationService()
                Log.d("LocationService", "📋 Total recordatorios en BD: ${allReminders.size}")

                // ✅ DEBUG: Mostrar estado de TODOS
                allReminders.forEach {
                    Log.d("LocationService", "   🔍 ID: ${it.id} | ${it.title}")
                    Log.d("LocationService", "      ✓ is_active: ${it.is_active}")
                    Log.d("LocationService", "      ✓ is_deleted: ${it.is_deleted}")
                    Log.d("LocationService", "      ✓ type: ${it.reminder_type}")
                }
                Log.d("LocationService", "")

                // ✅ Filtrar solo los ACTIVOS
                val activeReminders = allReminders.filter {
                    val isLocationType = (it.reminder_type == "location" || it.reminder_type == "both")
                    val hasCoords = it.latitude != null && it.longitude != null
                    val isActive = it.is_active == true
                    val notDeleted = it.is_deleted == false

                    val passes = isLocationType && hasCoords && isActive && notDeleted

                    if (!passes && isLocationType) {
                        Log.d("LocationService", "   ❌ ID ${it.id} rechazado:")
                        Log.d("LocationService", "      hasCoords: $hasCoords")
                        Log.d("LocationService", "      isActive: $isActive")
                        Log.d("LocationService", "      notDeleted: $notDeleted")
                    }

                    passes
                }

                Log.d("LocationService", "✅ Recordatorios ACTIVOS: ${activeReminders.size}")

                // ✅ IMPORTANTE: Limpiar geofences de recordatorios inactivos
                val inactiveIds = allReminders
                    .filter { !it.is_active || it.is_deleted }
                    .map { it.id }

                if (inactiveIds.isNotEmpty()) {
                    val removed = activeGeofences.intersect(inactiveIds.toSet())
                    if (removed.isNotEmpty()) {
                        Log.d("LocationService", "🧹 Limpiando geofences inactivos: $removed")
                        activeGeofences.removeAll(inactiveIds.toSet())
                    }
                }

                if (activeReminders.isEmpty()) {
                    Log.w("LocationService", "⚠️ NO HAY RECORDATORIOS ACTIVOS")
                    Log.d("LocationService", "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@launch
                }

                // ✅ Procesar solo los ACTIVOS
                for (reminder in activeReminders) {
                    try {
                        val reminderLat = reminder.latitude ?: continue
                        val reminderLon = reminder.longitude ?: continue

                        val distance = calcularDistancia(lat, lon, reminderLat, reminderLon)
                        val radius = reminder.radius ?: 100f

                        val inside = distance <= radius
                        val wasInside = activeGeofences.contains(reminder.id)

                        Log.d("LocationService", "")
                        Log.d("LocationService", "📌 ${reminder.title} (ID: ${reminder.id})")
                        Log.d("LocationService", "   Distancia: ${distance.toInt()}m / Radio: ${radius.toInt()}m")
                        Log.d("LocationService", "   Estado: inside=$inside | wasInside=$wasInside")
                        Log.d("LocationService", "   Trigger: ${reminder.trigger_type}")

                        when {
                            inside && !wasInside -> {
                                activeGeofences.add(reminder.id)
                                if (reminder.trigger_type == "enter" || reminder.trigger_type == "both") {
                                    Log.d("LocationService", "   🔔 DISPARANDO: ENTRADA")
                                    triggerLocationNotification(reminder, "Entraste en la zona")
                                } else {
                                    Log.d("LocationService", "   ⏭️ No dispara (trigger: ${reminder.trigger_type})")
                                }
                            }
                            !inside && wasInside -> {
                                activeGeofences.remove(reminder.id)
                                if (reminder.trigger_type == "exit" || reminder.trigger_type == "both") {
                                    Log.d("LocationService", "   🔔 DISPARANDO: SALIDA")
                                    triggerLocationNotification(reminder, "Saliste de la zona")
                                } else {
                                    Log.d("LocationService", "   ⏭️ No dispara (trigger: ${reminder.trigger_type})")
                                }
                            }
                            inside && wasInside -> {
                                Log.d("LocationService", "   ℹ️ Sigue dentro")
                            }
                            else -> {
                                Log.d("LocationService", "   ℹ️ Fuera de zona")
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("LocationService", "❌ Error ID ${reminder.id}: ${e.message}")
                    }
                }

                Log.d("LocationService", "🗺️ Geofences activos: $activeGeofences")
                Log.d("LocationService", "━━━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                Log.e("LocationService", "❌ ERROR CRÍTICO: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun triggerLocationNotification(reminder: ReminderEntity, transition: String) {
        Log.d("LocationService", "🔔 CREANDO NOTIFICACIÓN:")
        Log.d("LocationService", "   Título: ${reminder.title}")
        Log.d("LocationService", "   Transición: $transition")
        Log.d("LocationService", "   Estado activo: ${reminder.is_active}") // ✅ Log adicional

        NotificationHelper.createNotificationChannel(applicationContext)

        val builder = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(reminder.title)
            .setContentText("${reminder.description ?: "Sin descripción"} ($transition)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (reminder.sound) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            Log.d("LocationService", "   🔊 Sonido activado")
        }

        if (reminder.vibration) {
            builder.setVibrate(longArrayOf(0, 500, 200, 500))
            Log.d("LocationService", "   📳 Vibración activada")
        }

        val notificationId = Random.nextInt(1000, 9999)
        Log.d("LocationService", "   🆔 ID de notificación: $notificationId")

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
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}