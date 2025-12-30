package com.example.app.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.models.NotificationSound
import com.example.app.models.Reminder
import com.example.app.models.ReminderEntity
import com.example.app.models.toReminder
import com.example.app.models.toReminderRequest
import com.example.app.models.toReminderResponse
import com.example.app.network.RetrofitClient
import com.example.app.repository.ReminderRepository
import com.example.app.screen.recordatorios.components.ReminderReceiver
import com.example.app.screen.recordatorios.components.scheduleReminder
import com.example.app.services.LocationReminderService
import com.example.app.utils.PermissionUtils
import com.example.app.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val repository: ReminderRepository
) : ViewModel() {

    // Estados para la lista de recordatorios
    private val _reminders = MutableStateFlow<List<ReminderEntity>>(emptyList())

    var reminders by mutableStateOf<List<Reminder>>(emptyList())
        private set

    // Estados para UI
    var isLoading by mutableStateOf(false)
        private set

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Para preview de sonidos
    private var currentRingtone: Ringtone? = null
    private var playbackJob: Job? = null

    // Cargar recordatorios desde la base de datos local
    fun loadReminders() {
        viewModelScope.launch {
            try {
                isLoading = true
                _error.value = null
                val result = repository.getLocalReminders()
                _reminders.value = result
                Log.d("ReminderViewModel", "✅ ${result.size} recordatorios cargados")
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("ReminderViewModel", "❌ Error al cargar recordatorios: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Obtener recordatorios desde la API
    fun fetchReminders(token: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.reminderService.getReminders("Bearer $token")
                if (response.isSuccessful) {
                    val apiReminders = response.body() ?: emptyList()

                    // ✅ Convertir ReminderResponse a Reminder
                    reminders = apiReminders.map { it.toReminder() }

                    Log.d("ReminderViewModel", "✅ ${reminders.size} recordatorios cargados desde API")
                } else {
                    Log.e("ReminderVM", "Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("ReminderVM", "Error de red: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun createReminder(reminder: Reminder, context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                isLoading = true
                _error.value = null

                Log.d("ReminderViewModel", "🚀 INICIANDO creación de recordatorio: ${reminder.title}")

                val sessionManager = SessionManager.getInstance(context)
                val token = sessionManager.getAccessToken()
                val userId = sessionManager.getUser()?.id ?: 1

                var reminderId: Int? = null

                // 1️⃣ ENVIAR A LA API (OBLIGATORIO)
                if (token != null) {
                    try {
                        Log.d("ReminderViewModel", "🌐 Enviando recordatorio a la API...")

                        val reminderRequest = reminder.toReminderRequest()

                        val response = RetrofitClient.reminderService.createReminder(
                            "Bearer $token",
                            reminderRequest
                        )

                        if (response.isSuccessful && response.body() != null) {
                            reminderId = response.body()!!.id
                            Log.d("ReminderViewModel", "✅ Recordatorio creado en API con ID: $reminderId")
                        } else {
                            // ❌ ERROR EN API - DETENER EJECUCIÓN
                            val errorBody = response.errorBody()?.string()
                            val errorMessage = try {
                                val jsonError = org.json.JSONObject(errorBody ?: "{}")
                                val detail = jsonError.optString("detail", "Error desconocido")

                                // 🔥 Limpiar el mensaje de error para hacerlo más legible
                                when {
                                    detail.contains("Ya existe un recordatorio con ese título", ignoreCase = true) -> {
                                        "Ya existe un recordatorio con ese título"
                                    }
                                    detail.contains("400:", ignoreCase = true) -> {
                                        // Extraer solo el mensaje después de "400: "
                                        detail.substringAfter("400: ", detail).trim()
                                    }
                                    detail.contains("Error creating reminder:", ignoreCase = true) -> {
                                        // Extraer solo el mensaje después de "Error creating reminder: "
                                        detail.substringAfter("Error creating reminder:", "").trim()
                                    }
                                    else -> detail
                                }
                            } catch (e: Exception) {
                                errorBody ?: "Error ${response.code()}"
                            }

                            Log.e("ReminderViewModel", "❌ Error en API: ${response.code()}")
                            Log.e("ReminderViewModel", "   Detalle: $errorMessage")

                            _error.value = errorMessage
                            onComplete(false) // 🔥 Notificar fallo
                            return@launch
                        }
                    } catch (e: Exception) {
                        Log.e("ReminderViewModel", "❌ Error de red: ${e.message}")
                        e.printStackTrace()

                        val errorMsg = "Error de red: ${e.message}"
                        _error.value = errorMsg
                        onComplete(false) // 🔥 Notificar fallo
                        return@launch
                    }
                } else {
                    val errorMsg = "No hay sesión activa"
                    Log.e("ReminderViewModel", "❌ $errorMsg")
                    _error.value = errorMsg
                    onComplete(false) // 🔥 Notificar fallo
                    return@launch
                }

                // 2️⃣ GUARDAR LOCALMENTE (solo si API fue exitosa)
                val localId = reminderId ?: System.currentTimeMillis().toInt()
                Log.d("ReminderViewModel", "🔹 Creando ReminderEntity con ID: $localId")

                val daysString = reminder.days?.joinToString(",")

                val reminderEntity = ReminderEntity(
                    id = localId,
                    title = reminder.title,
                    description = reminder.description,
                    reminder_type = reminder.reminder_type,
                    trigger_type = reminder.trigger_type,
                    sound_uri = reminder.sound_uri,
                    vibration = reminder.vibration,
                    sound = reminder.sound,
                    days = daysString,
                    time = reminder.time,
                    location = reminder.location,
                    latitude = reminder.latitude,
                    longitude = reminder.longitude,
                    radius = reminder.radius?.toFloat(),
                    user_id = userId,
                    is_active = true,
                    is_deleted = false
                )

                Log.d("ReminderViewModel", "🔹 Guardando en repositorio...")
                repository.saveReminder(reminderEntity)
                Log.d("ReminderViewModel", "💾 Recordatorio guardado localmente")

                // 3️⃣ PROGRAMAR SEGÚN TIPO DE RECORDATORIO
                when (reminder.reminder_type) {
                    "datetime" -> {
                        Log.d("ReminderViewModel", "⏰ Tipo: DATETIME - Programando alarmas...")
                        programarAlarmasFechaHora(context, reminder, reminderEntity, localId)
                    }

                    "location" -> {
                        Log.d("ReminderViewModel", "📍 Tipo: LOCATION - Verificando permisos...")

                        if (PermissionUtils.hasLocationPermissions(context)) {
                            LocationReminderService.start(context)
                            Log.d("ReminderViewModel", "✅ Servicio de ubicación iniciado")
                        } else {
                            Log.w("ReminderViewModel", "⚠️ Sin permisos de ubicación - servicio NO iniciado")
                        }
                    }

                    "both" -> {
                        Log.d("ReminderViewModel", "🎯 Tipo: BOTH - Programando AMBOS sistemas...")

                        programarAlarmasFechaHora(context, reminder, reminderEntity, localId)

                        if (PermissionUtils.hasLocationPermissions(context)) {
                            LocationReminderService.start(context)
                            Log.d("ReminderViewModel", "✅ Servicio de ubicación iniciado para tipo BOTH")
                        } else {
                            Log.w("ReminderViewModel", "⚠️ Sin permisos de ubicación - solo alarmas programadas")
                        }
                    }
                }

                // 4️⃣ RECARGAR LA LISTA
                token?.let { fetchReminders(it) }

                Log.d("ReminderViewModel", "✅ Proceso completado: ${reminder.title}")
                onComplete(true) // 🔥 Notificar éxito

            } catch (e: Exception) {
                _error.value = "Error al crear recordatorio: ${e.message}"
                Log.e("ReminderViewModel", "❌ Error COMPLETO: ${e.message}", e)
                e.printStackTrace()
                onComplete(false) // 🔥 Notificar fallo
            } finally {
                isLoading = false
            }
        }
    }

    private fun programarAlarmasFechaHora(
        context: Context,
        reminder: Reminder,
        reminderEntity: ReminderEntity,
        localId: Int
    ) {
        if (reminder.days.isNullOrEmpty() || reminder.time.isNullOrEmpty()) {
            Log.w("ReminderViewModel", "⚠️ No se pueden programar alarmas: días o tiempo faltantes")
            return
        }

        Log.d("ReminderViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("ReminderViewModel", "⏰ PROGRAMANDO ALARMAS DE FECHA/HORA")
        Log.d("ReminderViewModel", "   Total de días: ${reminder.days.size}")
        Log.d("ReminderViewModel", "   Hora: ${reminder.time}")

        reminder.days.forEachIndexed { index, day ->
            val uniqueId = localId * 100 + index

            val singleDayReminder = reminderEntity.copy(
                id = uniqueId,
                days = day  // ← UN SOLO DÍA como String
            )

            Log.d("ReminderViewModel", "")
            Log.d("ReminderViewModel", "📅 Alarma #${index + 1}:")
            Log.d("ReminderViewModel", "   Día: '$day'")
            Log.d("ReminderViewModel", "   Hora: ${reminder.time}")
            Log.d("ReminderViewModel", "   ID único: $uniqueId")

            scheduleReminder(context, singleDayReminder)
        }

        Log.d("ReminderViewModel", "")
        Log.d("ReminderViewModel", "✅ ${reminder.days.size} alarmas programadas exitosamente")
        Log.d("ReminderViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    fun toggleReminderActive(reminderId: Int, active: Boolean, context: Context) {
        viewModelScope.launch {
            val sessionManager = SessionManager.getInstance(context)
            val token = sessionManager.getAccessToken()

            if (!token.isNullOrEmpty()) {
                try {
                    val response = RetrofitClient.reminderService.toggleReminder(
                        "Bearer $token",
                        reminderId
                    )
                    if (response.isSuccessful) {
                        Log.d("ReminderViewModel", "✅ Toggle exitoso")

                        // ✅ La respuesta es ReminderResponse
                        val reminderResponse = response.body()
                        Log.d("ReminderViewModel", "   is_active: ${reminderResponse?.is_active}")

                        repository.setReminderActive(reminderId, active)
                        fetchReminders(token)
                    } else {
                        Log.e("ReminderViewModel", "⚠️ Error API toggle: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("ReminderViewModel", "⚠️ Error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    fun deleteReminder(context: Context, reminderId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                isLoading = true
                Log.d("ReminderViewModel", "🗑️ Eliminando recordatorio ID: $reminderId")

                val sessionManager = SessionManager.getInstance(context)
                val token = sessionManager.getAccessToken()

                // 1️⃣ Eliminar en la API
                if (!token.isNullOrEmpty()) {
                    try {
                        val response = RetrofitClient.reminderService.deleteReminder(
                            "Bearer $token",
                            reminderId
                        )
                        if (response.isSuccessful) {
                            Log.d("ReminderViewModel", "✅ Recordatorio eliminado en API")

                            // 2️⃣ Eliminar localmente
                            repository.deleteReminderById(reminderId)

                            // 3️⃣ Recargar desde API
                            fetchReminders(token)

                            Toast.makeText(context, "Recordatorio eliminado", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        } else {
                            Log.e("ReminderViewModel", "⚠️ Error API eliminar: ${response.code()}")
                            Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("ReminderViewModel", "⚠️ Error de red: ${e.message}")
                        Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ReminderViewModel", "❌ Error al eliminar: ${e.message}")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }
    // Preview de sonidos
    fun playPreviewSound(context: Context, soundUri: String) {
        playbackJob?.cancel()
        currentRingtone?.stop()

        playbackJob = viewModelScope.launch {
            try {
                Log.d("ReminderViewModel", "Reproduciendo sonido con URI: $soundUri")
                currentRingtone = RingtoneManager.getRingtone(context, Uri.parse(soundUri))
                currentRingtone?.play()

                delay(2000)
                if (currentRingtone?.isPlaying == true) {
                    currentRingtone?.stop()
                }
            } catch (e: Exception) {
                Log.e("ReminderViewModel", "Error al reproducir sonido: ${e.message}")
            }
        }
    }

    fun updateReminder(
        reminderId: Int,
        reminder: Reminder,
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                isLoading = true
                _error.value = null
                val reminderRequest = reminder.toReminderRequest()

                Log.d("ReminderViewModel", "🔄 ACTUALIZANDO recordatorio ID: $reminderId")


                val sessionManager = SessionManager.getInstance(context)
                val token = sessionManager.getAccessToken()
                val userId = sessionManager.getUser()?.id ?: 1


                Log.d("ReminderViewModel", "🔵 ReminderRequest que se enviará:")
                Log.d("ReminderViewModel", "   reminder_type = ${reminderRequest.reminder_type}")
                Log.d("ReminderViewModel", "   title = ${reminderRequest.title}")
                Log.d("ReminderViewModel", "   days = ${reminderRequest.days}")
                Log.d("ReminderViewModel", "   JSON = ${com.google.gson.Gson().toJson(reminderRequest)}")


                // 1️⃣ ACTUALIZAR EN LA API
                if (token != null) {
                    try {
                        val reminderRequest = reminder.toReminderRequest()

                        val response = RetrofitClient.reminderService.updateReminder(
                            "Bearer $token",
                            reminderId,
                            reminderRequest
                        )

                        if (response.isSuccessful && response.body() != null) {
                            Log.d("ReminderViewModel", "✅ Recordatorio actualizado en API")

                            // 2️⃣ CANCELAR ALARMAS ANTIGUAS
                            cancelReminderAlarms(context, reminderId)

                            // 3️⃣ ACTUALIZAR LOCALMENTE
                            val daysString = reminder.days?.joinToString(",")

                            val reminderEntity = ReminderEntity(
                                id = reminderId,
                                title = reminder.title,
                                description = reminder.description,
                                reminder_type = reminder.reminder_type,
                                trigger_type = reminder.trigger_type,
                                sound_uri = reminder.sound_uri,
                                vibration = reminder.vibration,
                                sound = reminder.sound,
                                days = daysString,
                                time = reminder.time,
                                location = reminder.location,
                                latitude = reminder.latitude,
                                longitude = reminder.longitude,
                                radius = reminder.radius?.toFloat(),
                                user_id = userId,
                                is_active = true,
                                is_deleted = false
                            )

                            repository.updateReminder(reminderEntity)

                            // 4️⃣ REPROGRAMAR ALARMAS NUEVAS
                            when (reminder.reminder_type) {
                                "datetime" -> {
                                    programarAlarmasFechaHora(context, reminder, reminderEntity, reminderId)
                                }
                                "location" -> {
                                    if (PermissionUtils.hasLocationPermissions(context)) {
                                        LocationReminderService.start(context)
                                    }
                                }
                                "both" -> {
                                    programarAlarmasFechaHora(context, reminder, reminderEntity, reminderId)
                                    if (PermissionUtils.hasLocationPermissions(context)) {
                                        LocationReminderService.start(context)
                                    }
                                }
                            }

                            // 5️⃣ RECARGAR LISTA
                            fetchReminders(token)

                            onComplete(true)

                        } else {
                            val errorBody = response.errorBody()?.string()
                            val errorMessage = try {
                                val jsonError = org.json.JSONObject(errorBody ?: "{}")
                                jsonError.optString("detail", "Error al actualizar")
                            } catch (e: Exception) {
                                "Error ${response.code()}"
                            }

                            _error.value = errorMessage
                            onComplete(false)
                        }

                    } catch (e: Exception) {
                        Log.e("ReminderViewModel", "❌ Error de red: ${e.message}")
                        _error.value = "Error de red: ${e.message}"
                        onComplete(false)
                    }
                } else {
                    _error.value = "No hay sesión activa"
                    onComplete(false)
                }

            } catch (e: Exception) {
                _error.value = "Error al actualizar: ${e.message}"
                Log.e("ReminderViewModel", "❌ Error: ${e.message}", e)
                onComplete(false)
            } finally {
                isLoading = false
            }
        }
    }

    // 🆕 Función para obtener un recordatorio por ID desde la base de datos
    fun getReminderById(reminderId: Int, onResult: (ReminderEntity?) -> Unit) {
        viewModelScope.launch {
            try {
                val reminderEntity = repository.getReminderById(reminderId)
                onResult(reminderEntity)
            } catch (e: Exception) {
                Log.e("ReminderViewModel", "❌ Error obteniendo recordatorio: ${e.message}")
                onResult(null)
            }
        }
    }

    // 🔥 Función auxiliar para cancelar alarmas
    private fun cancelReminderAlarms(context: Context, reminderId: Int) {
        try {
            // Cancelar hasta 7 alarmas (una por día de la semana)
            for (i in 0..6) {
                val uniqueId = reminderId * 100 + i
                cancelAlarm(context, uniqueId)
            }
            Log.d("ReminderViewModel", "🚫 Alarmas canceladas para ID: $reminderId")
        } catch (e: Exception) {
            Log.e("ReminderViewModel", "Error cancelando alarmas: ${e.message}")
        }
    }

    fun cancelAllReminders(context: Context) {
        viewModelScope.launch {
            try {
                Log.d("ReminderViewModel", "🧹 Cancelando todas las alarmas...")

                // Obtener todos los recordatorios locales
                val allReminders = repository.getLocalReminders()

                allReminders.forEach { reminder ->
                    // Cancelar alarmas de fecha/hora
                    if (reminder.reminder_type == "datetime" || reminder.reminder_type == "both") {
                        val days = reminder.days?.split(",") ?: emptyList()
                        days.forEachIndexed { index, _ ->
                            val uniqueId = reminder.id * 100 + index
                            cancelAlarm(context, uniqueId)
                        }
                    }
                }

                // Detener servicio de ubicación
                LocationReminderService.stop(context)

                // Limpiar base de datos local
                repository.clearAllReminders()

                Log.d("ReminderViewModel", "✅ Todas las alarmas canceladas y BD limpiada")
            } catch (e: Exception) {
                Log.e("ReminderViewModel", "❌ Error al cancelar alarmas: ${e.message}")
            }
        }
    }

    private fun cancelAlarm(context: Context, reminderId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        Log.d("ReminderViewModel", "🚫 Alarma cancelada - ID: $reminderId")
    }

    // Limpiar error
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        currentRingtone?.stop()
    }
}

// Factory para crear el ViewModel
class ReminderViewModelFactory(
    private val repository: ReminderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun rememberSystemNotificationSounds(context: Context): List<NotificationSound> {
    return remember {
        val sounds = mutableListOf<NotificationSound>()

        // Agregar el tono predeterminado del sistema
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        sounds.add(NotificationSound(
            uri = defaultUri.toString(),
            title = "Predeterminado del sistema"
        ))

        // Obtener todos los tonos de notificación disponibles
        val ringtoneManager = RingtoneManager(context)
        ringtoneManager.setType(RingtoneManager.TYPE_NOTIFICATION)
        val cursor = ringtoneManager.cursor

        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = ringtoneManager.getRingtoneUri(cursor.position)
            sounds.add(NotificationSound(
                uri = uri.toString(),
                title = title
            ))
        }
        cursor.close()

        sounds
    }
}