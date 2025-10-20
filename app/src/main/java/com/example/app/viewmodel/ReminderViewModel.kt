package com.example.app.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.models.Reminder
import com.example.app.models.ReminderEntity
import com.example.app.models.toReminder
import com.example.app.models.toReminderResponse
import com.example.app.network.RetrofitClient
import com.example.app.repository.ReminderRepository
import com.example.app.screen.recordatorios.components.ReminderReceiver
import com.example.app.screen.recordatorios.components.scheduleReminder
import com.example.app.services.LocationReminderService
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

    fun createReminder(reminder: Reminder, context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                isLoading = true
                _error.value = null

                Log.d("ReminderViewModel", "🚀 INICIANDO creación de recordatorio: ${reminder.title}")

                val sessionManager = SessionManager(context)
                val token = sessionManager.getAccessToken()
                val userId = sessionManager.getUser()?.id ?: 1

                var reminderId: Int? = null

                // 1️⃣ ENVIAR A LA API (OBLIGATORIO)
                if (token != null) {
                    try {
                        Log.d("ReminderViewModel", "🌐 Enviando recordatorio a la API...")

                        val reminderRequest = reminder.toReminderResponse()

                        val response = RetrofitClient.reminderService.createReminder(
                            "Bearer $token",
                            reminderRequest
                        )

                        if (response.isSuccessful && response.body() != null) {
                            reminderId = response.body()!!.id
                            Log.d("ReminderViewModel", "✅ Recordatorio creado en API con ID: $reminderId")
                            Toast.makeText(context, "Recordatorio creado", Toast.LENGTH_SHORT).show()
                        } else {
                            // ❌ ERROR EN API - DETENER EJECUCIÓN
                            val errorBody = response.errorBody()?.string()
                            val errorMessage = try {
                                // Intentar parsear el JSON de error
                                val jsonError = org.json.JSONObject(errorBody ?: "{}")
                                jsonError.optString("detail", "Error desconocido")
                            } catch (e: Exception) {
                                errorBody ?: "Error ${response.code()}"
                            }

                            Log.e("ReminderViewModel", "❌ Error en API: ${response.code()}")
                            Log.e("ReminderViewModel", "   Detalle: $errorMessage")

                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()

                            // ✅ IMPORTANTE: Detener ejecución
                            _error.value = errorMessage
                            return@launch  // ← SALIR AQUÍ, NO CONTINUAR
                        }
                    } catch (e: Exception) {
                        Log.e("ReminderViewModel", "❌ Error de red: ${e.message}")
                        e.printStackTrace()

                        val errorMsg = "Error de red: ${e.message}"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        _error.value = errorMsg

                        // ✅ IMPORTANTE: Detener ejecución
                        return@launch  // ← SALIR AQUÍ, NO CONTINUAR
                    }
                } else {
                    // Sin token
                    val errorMsg = "No hay sesión activa"
                    Log.e("ReminderViewModel", "❌ $errorMsg")
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    _error.value = errorMsg
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
                    sound_type = reminder.sound_type,
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
                        Log.d("ReminderViewModel", "📍 Tipo: LOCATION - Iniciando servicio de ubicación...")
                        LocationReminderService.start(context)
                        Log.d("ReminderViewModel", "✅ Servicio de ubicación iniciado")
                    }

                    "both" -> {
                        Log.d("ReminderViewModel", "🎯 Tipo: BOTH - Programando AMBOS sistemas...")

                        // Programar alarmas de fecha/hora
                        programarAlarmasFechaHora(context, reminder, reminderEntity, localId)

                        // Iniciar servicio de ubicación
                        LocationReminderService.start(context)
                        Log.d("ReminderViewModel", "✅ Servicio de ubicación iniciado para tipo BOTH")
                    }
                }

                // 4️⃣ RECARGAR LA LISTA
                token?.let { fetchReminders(it) }

                Log.d("ReminderViewModel", "✅ Proceso completado: ${reminder.title}")
                onSuccess()

            } catch (e: Exception) {
                _error.value = "Error al crear recordatorio: ${e.message}"
                Log.e("ReminderViewModel", "❌ Error COMPLETO: ${e.message}", e)
                e.printStackTrace()
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
            val sessionManager = SessionManager(context)
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

                val sessionManager = SessionManager(context)
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
    fun playPreviewSound(context: Context, soundType: String) {
        playbackJob?.cancel()
        currentRingtone?.stop()

        playbackJob = viewModelScope.launch {
            try {
                val soundUri = when (soundType) {
                    "default" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    "gentle" -> {
                        val ringtoneManager = RingtoneManager(context)
                        ringtoneManager.setType(RingtoneManager.TYPE_NOTIFICATION)
                        val cursor = ringtoneManager.cursor
                        if (cursor.moveToPosition(0)) {
                            ringtoneManager.getRingtoneUri(0)
                        } else {
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        }
                    }
                    "alert" -> {
                        RingtoneManager.getActualDefaultRingtoneUri(
                            context,
                            RingtoneManager.TYPE_ALARM
                        ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }
                    "chime" -> {
                        val ringtoneManager = RingtoneManager(context)
                        ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE)
                        val cursor = ringtoneManager.cursor
                        if (cursor.moveToPosition(0)) {
                            ringtoneManager.getRingtoneUri(0)
                        } else {
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        }
                    }
                    else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }

                Log.d("ReminderViewModel", "Reproduciendo sonido: $soundType con URI: $soundUri")
                currentRingtone = RingtoneManager.getRingtone(context, soundUri)
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