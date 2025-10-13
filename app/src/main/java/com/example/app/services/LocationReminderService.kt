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

                val allReminders = repository.getLocalReminders()
                Log.d("LocationService", "📋 Total recordatorios en BD: ${allReminders.size}")

                // Debug detallado de TODOS los recordatorios
                allReminders.forEachIndexed { index, r ->
                    Log.d("LocationService", "")
                    Log.d("LocationService", "🔎 Recordatorio [$index]:")
                    Log.d("LocationService", "   ID: ${r.id}")
                    Log.d("LocationService", "   Título: ${r.title}")
                    Log.d("LocationService", "   Tipo: ${r.reminder_type}")
                    Log.d("LocationService", "   Trigger: ${r.trigger_type}")
                    Log.d("LocationService", "   Latitud: ${r.latitude}")
                    Log.d("LocationService", "   Longitud: ${r.longitude}")
                    Log.d("LocationService", "   Radio: ${r.radius}")
                }

                val reminders = allReminders.filter {
                    val isLocationType = (it.reminder_type == "location" || it.reminder_type == "both")
                    val hasCoords = it.latitude != null && it.longitude != null

                    Log.d("LocationService", "")
                    Log.d("LocationService", "🔍 Evaluando ID=${it.id}:")
                    Log.d("LocationService", "   ¿Es tipo ubicación? $isLocationType")
                    Log.d("LocationService", "   ¿Tiene coordenadas? $hasCoords")
                    Log.d("LocationService", "   ¿Pasa filtro? ${isLocationType && hasCoords}")

                    isLocationType && hasCoords
                }

                Log.d("LocationService", "")
                Log.d("LocationService", "✅ Recordatorios que pasaron filtro: ${reminders.size}")

                if (reminders.isEmpty()) {
                    Log.w("LocationService", "⚠️ NO HAY RECORDATORIOS VÁLIDOS PARA PROCESAR")
                    Log.d("LocationService", "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@launch
                }

                for (reminder in reminders) {
                    try {
                        Log.d("LocationService", "")
                        Log.d("LocationService", "🎯 PROCESANDO:")
                        Log.d("LocationService", "   ID: ${reminder.id}")
                        Log.d("LocationService", "   Título: ${reminder.title}")

                        val reminderLat = reminder.latitude
                        val reminderLon = reminder.longitude

                        if (reminderLat == null || reminderLon == null) {
                            Log.e("LocationService", "   ❌ ERROR: Coordenadas nulas")
                            continue
                        }

                        Log.d("LocationService", "   📍 Centro objetivo: $reminderLat, $reminderLon")
                        Log.d("LocationService", "   📍 Tu ubicación: $lat, $lon")

                        val distance = calcularDistancia(lat, lon, reminderLat, reminderLon)
                        val radius = reminder.radius ?: 100f

                        Log.d("LocationService", "   📏 Distancia calculada: ${distance.toInt()} metros")
                        Log.d("LocationService", "   ⭕ Radio configurado: ${radius.toInt()} metros")

                        val inside = distance <= radius
                        val wasInside = activeGeofences.contains(reminder.id)

                        Log.d("LocationService", "   🎯 ¿Estás dentro? $inside")
                        Log.d("LocationService", "   🕐 ¿Estabas dentro antes? $wasInside")
                        Log.d("LocationService", "   🔔 Trigger configurado: ${reminder.trigger_type}")

                        when {
                            inside && !wasInside -> {
                                // ENTRANDO
                                Log.d("LocationService", "   🚪 DETECTADO: ENTRASTE AL ÁREA")
                                activeGeofences.add(reminder.id)

                                if (reminder.trigger_type == "enter" || reminder.trigger_type == "both") {
                                    Log.d("LocationService", "   ✅ Trigger coincide, enviando notificación...")
                                    triggerLocationNotification(reminder, "Entraste en la zona")
                                } else {
                                    Log.d("LocationService", "   ⚠️ Trigger no coincide (necesita: ${reminder.trigger_type})")
                                }
                            }
                            !inside && wasInside -> {
                                // SALIENDO
                                Log.d("LocationService", "   🏃 DETECTADO: SALISTE DEL ÁREA")
                                activeGeofences.remove(reminder.id)

                                if (reminder.trigger_type == "exit" || reminder.trigger_type == "both") {
                                    Log.d("LocationService", "   ✅ Trigger coincide, enviando notificación...")
                                    triggerLocationNotification(reminder, "Saliste de la zona")
                                } else {
                                    Log.d("LocationService", "   ⚠️ Trigger no coincide (necesita: ${reminder.trigger_type})")
                                }
                            }
                            inside && wasInside -> {
                                Log.d("LocationService", "   ℹ️ Sigues dentro del área (sin cambios)")
                            }
                            else -> {
                                Log.d("LocationService", "   ℹ️ Sigues fuera del área (sin cambios)")
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("LocationService", "❌ Error procesando ID ${reminder.id}: ${e.message}")
                        Log.e("LocationService", e.stackTraceToString())
                    }
                }

                Log.d("LocationService", "━━━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                Log.e("LocationService", "❌ ERROR CRÍTICO: ${e.message}")
                Log.e("LocationService", e.stackTraceToString())
            }
        }
    }

    private fun triggerLocationNotification(reminder: ReminderEntity, transition: String) {
        Log.d("LocationService", "🔔 CREANDO NOTIFICACIÓN:")
        Log.d("LocationService", "   Título: ${reminder.title}")
        Log.d("LocationService", "   Transición: $transition")

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