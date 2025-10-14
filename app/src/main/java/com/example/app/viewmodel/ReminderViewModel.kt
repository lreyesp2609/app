package com.example.app.viewmodel

import android.app.NotificationManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.models.Reminder
import com.example.app.models.ReminderEntity
import com.example.app.network.RetrofitClient
import com.example.app.screen.recordatorios.components.scheduleReminder
import com.example.app.utils.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

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

    // Crear recordatorio (envía a API + guarda local + programa notificación)
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

                // 1️⃣ ENVIAR A LA API (requiere internet)
                if (token != null) {
                    try {
                        Log.d("ReminderViewModel", "🌐 Enviando recordatorio a la API...")
                        val response = RetrofitClient.reminderService.createReminder(
                            "Bearer $token",
                            reminder
                        )

                        if (response.isSuccessful && response.body() != null) {
                            reminderId = response.body()!!.id
                            Log.d("ReminderViewModel", "✅ Recordatorio creado en API con ID: $reminderId")
                            Toast.makeText(context, "Recordatorio creado", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("ReminderViewModel", "⚠️ Error en API: ${response.code()}")
                            val error = response.errorBody()?.string()
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("ReminderViewModel", "⚠️ Error al enviar a API: ${e.message}")
                        Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                // 2️⃣ GUARDAR LOCALMENTE (siempre se hace)
                val localId = reminderId ?: System.currentTimeMillis().toInt()
                Log.d("ReminderViewModel", "🔹 Creando ReminderEntity con ID: $localId")

                // Convertir List<String> a String separado por comas
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
                    user_id = userId
                )

                Log.d("ReminderViewModel", "🔹 Guardando en repositorio...")
                repository.saveReminder(reminderEntity)
                Log.d("ReminderViewModel", "💾 Recordatorio guardado localmente")

                // 3️⃣ PROGRAMAR ALARMA - ⚠️ SOLO para recordatorios con TIEMPO
                Log.d("ReminderViewModel", "🔹 Verificando si programar alarma...")
                Log.d("ReminderViewModel", "   reminder_type: ${reminder.reminder_type}")
                Log.d("ReminderViewModel", "   días: ${reminder.days}, hora: ${reminder.time}")

                val shouldScheduleAlarm = when (reminder.reminder_type) {
                    "datetime" -> true  // ✅ Fecha y hora
                    "location" -> false // Solo ubicación
                    "both" -> true      // Tiempo + ubicación
                    else -> false
                }

                if (shouldScheduleAlarm) {
                    // Validar que tenga días Y hora
                    if (!reminder.days.isNullOrEmpty() && !reminder.time.isNullOrEmpty()) {
                        Log.d("ReminderViewModel", "⏰ Programando alarmas para ${reminder.days.size} días...")

                        // ✅ PROGRAMAR UNA ALARMA POR CADA DÍA
                        reminder.days.forEachIndexed { index, day ->
                            // Crear un ReminderEntity temporal con un solo día
                            // Usar IDs únicos para cada día (base_id * 100 + index)
                            val uniqueId = localId * 100 + index

                            val singleDayReminder = reminderEntity.copy(
                                id = uniqueId,  // ID único para cada alarma
                                days = day      // Un solo día: "Lunes", "Martes", etc.
                            )

                            scheduleReminder(context, singleDayReminder)
                            Log.d("ReminderViewModel", "   ✅ Alarma $index: $day a las ${reminder.time} (ID: $uniqueId)")
                        }

                        Log.d("ReminderViewModel", "✅ ${reminder.days.size} alarmas programadas exitosamente")
                    } else {
                        Log.w("ReminderViewModel", "⚠️ No se puede programar alarma: faltan días u hora")
                        Log.w("ReminderViewModel", "   Días recibidos: ${reminder.days}")
                        Log.w("ReminderViewModel", "   Hora recibida: ${reminder.time}")
                    }
                } else {
                    Log.d("ReminderViewModel", "ℹ️ No se programa alarma (tipo: ${reminder.reminder_type})")
                    Log.d("ReminderViewModel", "📍 El LocationReminderService manejará este recordatorio")
                }

                // 4️⃣ RECARGAR LA LISTA
                loadReminders()

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

    // Obtener recordatorios desde la API
    fun fetchReminders(token: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.reminderService.getReminders("Bearer $token")
                if (response.isSuccessful) {
                    reminders = response.body() ?: emptyList()
                } else {
                    Log.e("ReminderVM", "Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("ReminderVM", "Error de red: ${e.message}")
            }
            isLoading = false
        }
    }

    // Eliminar recordatorio
    fun deleteReminder(context: Context, reminderId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                isLoading = true

                Log.d("ReminderViewModel", "🗑️ Eliminando recordatorio ID: $reminderId")

                // 2️⃣ Eliminar de la base de datos local
                repository.deleteReminderById(reminderId)

                Log.d("ReminderViewModel", "✅ Recordatorio eliminado")
                Toast.makeText(context, "Recordatorio eliminado", Toast.LENGTH_SHORT).show()

                // 3️⃣ Recargar lista
                loadReminders()

                onSuccess()

            } catch (e: Exception) {
                Log.e("ReminderViewModel", "❌ Error al eliminar: ${e.message}")
                Toast.makeText(context, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
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