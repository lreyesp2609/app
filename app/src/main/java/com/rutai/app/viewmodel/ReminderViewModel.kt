package com.rutai.app.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.rutai.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rutai.app.BaseViewModel
import com.rutai.app.models.NotificationSound
import com.rutai.app.models.Reminder
import com.rutai.app.models.ReminderEntity
import com.rutai.app.models.toReminder
import com.rutai.app.models.toReminderRequest
import com.rutai.app.repository.ReminderRepository
import com.rutai.app.screen.recordatorios.components.ReminderReceiver
import com.rutai.app.screen.recordatorios.components.scheduleReminder
import com.rutai.app.services.UnifiedLocationService
import com.rutai.app.utils.PermissionUtils
import com.rutai.app.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReminderViewModel(
    context: Context,
    private val repository: ReminderRepository,
    sessionManager: SessionManager
) : BaseViewModel(context, sessionManager) {

    var reminders by mutableStateOf<List<Reminder>>(emptyList())
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
                // _reminders.value = result // Si es necesario un StateFlow local
                Log.d("ReminderViewModel", "✅ ${result.size} recordatorios cargados localmente")
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("ReminderViewModel", "❌ Error al cargar recordatorios: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Obtener recordatorios desde la API
    fun fetchReminders() {
        safeApiCall(
            call = { token -> repository.fetchReminders(token) },
            onSuccess = { apiReminders ->
                reminders = apiReminders.map { it.toReminder() }
                Log.d("ReminderViewModel", "✅ ${reminders.size} recordatorios cargados desde API")
            },
            onError = { errorMsg ->
                Log.e("ReminderVM", "Error: $errorMsg")
                _error.value = errorMsg
            }
        )
    }

    fun createReminder(reminder: Reminder, context: Context, onComplete: (Boolean) -> Unit) {
        Log.d("ReminderViewModel", "🚀 INICIANDO creación de recordatorio: ${reminder.title}")
        
        safeApiCall(
            call = { token -> repository.createReminder(token, reminder.toReminderRequest()) },
            onSuccess = { response ->
                val reminderId = response.id
                val userId = sessionManager.getUser()?.id ?: 1
                val localId = reminderId
                
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

                viewModelScope.launch {
                    repository.saveReminder(reminderEntity)
                    
                    // PROGRAMAR SEGÚN TIPO
                    when (reminder.reminder_type) {
                        "datetime" -> programarAlarmasFechaHora(context, reminder, reminderEntity, localId)
                        "location" -> {
                            if (PermissionUtils.hasLocationPermissions(context)) {
                                UnifiedLocationService.start(context)
                            }
                        }
                        "both" -> {
                            programarAlarmasFechaHora(context, reminder, reminderEntity, localId)
                            if (PermissionUtils.hasLocationPermissions(context)) {
                                UnifiedLocationService.start(context)
                            }
                        }
                    }
                    
                    fetchReminders()
                    onComplete(true)
                }
            },
            onError = { errorMsg ->
                _error.value = errorMsg
                onComplete(false)
            }
        )
    }

    private fun programarAlarmasFechaHora(
        context: Context,
        reminder: Reminder,
        reminderEntity: ReminderEntity,
        localId: Int
    ) {
        if (reminder.days.isNullOrEmpty() || reminder.time.isNullOrEmpty()) return

        reminder.days.forEachIndexed { index, day ->
            val uniqueId = localId * 100 + index
            val singleDayReminder = reminderEntity.copy(id = uniqueId, days = day)
            scheduleReminder(context, singleDayReminder)
        }
    }

    fun toggleReminderActive(reminderId: Int, active: Boolean, context: Context) {
        safeApiCall(
            call = { token -> repository.toggleReminder(token, reminderId) },
            onSuccess = {
                viewModelScope.launch {
                    repository.setReminderActive(reminderId, active)
                    fetchReminders()
                }
            },
            onError = { _error.value = it }
        )
    }

    fun deleteReminder(context: Context, reminderId: Int, onSuccess: () -> Unit) {
        safeApiCall(
            call = { token -> repository.deleteReminder(token, reminderId) },
            onSuccess = {
                viewModelScope.launch {
                    repository.deleteReminderById(reminderId)
                    fetchReminders()
                    Toast.makeText(context, context.getString(R.string.success_reminder_deleted), Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
            },
            onError = { errorMsg ->
                _error.value = errorMsg
                Toast.makeText(context, context.getString(R.string.error_reminder_delete), Toast.LENGTH_SHORT).show()
            }
        )
    }

    fun updateReminder(
        reminderId: Int,
        reminder: Reminder,
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        safeApiCall(
            call = { token -> repository.updateReminderApi(token, reminderId, reminder.toReminderRequest()) },
            onSuccess = { response ->
                viewModelScope.launch {
                    cancelReminderAlarms(context, reminderId)
                    val userId = sessionManager.getUser()?.id ?: 1
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

                    when (reminder.reminder_type) {
                        "datetime" -> programarAlarmasFechaHora(context, reminder, reminderEntity, reminderId)
                        "location", "both" -> {
                            if (reminder.reminder_type == "both") programarAlarmasFechaHora(context, reminder, reminderEntity, reminderId)
                            if (PermissionUtils.hasLocationPermissions(context)) {
                                UnifiedLocationService.start(context)
                            }
                        }
                    }
                    fetchReminders()
                    onComplete(true)
                }
            },
            onError = {
                _error.value = it
                onComplete(false)
            }
        )
    }

    fun getReminderById(reminderId: Int, onResult: (ReminderEntity?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getReminderById(reminderId))
        }
    }

    private fun cancelReminderAlarms(context: Context, reminderId: Int) {
        for (i in 0..6) {
            cancelAlarm(context, reminderId * 100 + i)
        }
    }

    fun cancelAllReminders(context: Context) {
        viewModelScope.launch {
            val allReminders = repository.getLocalReminders()
            allReminders.forEach { reminder ->
                if (reminder.reminder_type == "datetime" || reminder.reminder_type == "both") {
                    val days = reminder.days?.split(",") ?: emptyList()
                    days.forEachIndexed { index, _ ->
                        cancelAlarm(context, reminder.id * 100 + index)
                    }
                }
            }
            UnifiedLocationService.stop(context)
            repository.clearAllReminders()
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
    }

    fun playPreviewSound(context: Context, soundUri: String) {
        playbackJob?.cancel()
        currentRingtone?.stop()

        playbackJob = viewModelScope.launch {
            try {
                val uri = Uri.parse(soundUri)
                currentRingtone = RingtoneManager.getRingtone(context, uri)
                if (currentRingtone == null) return@launch

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    currentRingtone?.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }

                currentRingtone?.play()
                delay(3000)
                if (currentRingtone?.isPlaying == true) currentRingtone?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearError() { _error.value = null }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        currentRingtone?.stop()
    }
}

class ReminderViewModelFactory(
    private val context: Context,
    private val repository: ReminderRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderViewModel(context, repository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun rememberSystemNotificationSounds(context: Context): List<NotificationSound> {
    return remember {
        val sounds = mutableListOf<NotificationSound>()
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        sounds.add(NotificationSound(
            uri = defaultUri.toString(),
            title = context.getString(R.string.sound_default_system)
        ))
        val ringtoneManager = RingtoneManager(context)
        ringtoneManager.setType(RingtoneManager.TYPE_NOTIFICATION)
        val cursor = ringtoneManager.cursor
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = ringtoneManager.getRingtoneUri(cursor.position)
            sounds.add(NotificationSound(uri = uri.toString(), title = title))
        }
        cursor.close()
        sounds
    }
}
