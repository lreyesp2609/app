package com.example.app.screen.recordatorios.ViewModel

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.models.Reminder
import com.example.app.network.RetrofitClient
import com.example.app.viewmodel.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReminderViewModel : ViewModel() {

    private var currentRingtone: Ringtone? = null
    private var playbackJob: Job? = null

    var reminders by mutableStateOf<List<Reminder>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun playPreviewSound(context: Context, soundType: String) {
        // Cancelar reproducción anterior
        playbackJob?.cancel()
        currentRingtone?.stop()

        playbackJob = viewModelScope.launch {
            try {
                val soundUri = when (soundType) {
                    "default" -> {
                        // Tono de notificación predeterminado
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }

                    "gentle" -> {
                        // Intentar obtener un tono diferente de notificación (índice 0)
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
                        // Tono de alarma (más fuerte y diferente)
                        RingtoneManager.getActualDefaultRingtoneUri(
                            context,
                            RingtoneManager.TYPE_ALARM
                        ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }

                    "chime" -> {
                        // Tono de llamada (campanilla)
                        val ringtoneManager = RingtoneManager(context)
                        ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE)

                        // Intentar obtener el primer ringtone disponible
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

                // Detener después de 2 segundos
                delay(2000)
                if (currentRingtone?.isPlaying == true) {
                    currentRingtone?.stop()
                }

            } catch (e: Exception) {
                Log.e("ReminderViewModel", "Error al reproducir sonido: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        currentRingtone?.stop()
    }

    fun createReminder(reminder: Reminder, context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = "Bearer " + getTokenFromPrefs(context)

                val response = RetrofitClient.reminderService.createReminder(token, reminder)

                if (response.isSuccessful) {
                    Toast.makeText(context, "Recordatorio creado", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    val error = response.errorBody()?.string()
                    Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getTokenFromPrefs(context: Context): String {
        val sessionManager = SessionManager(context)
        return sessionManager.getAccessToken() ?: ""
    }

    fun fetchReminders(token: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.reminderService.getReminders("Bearer $token")
                if (response.isSuccessful) {
                    reminders = response.body() ?: emptyList()
                } else {
                    // Manejar error
                    Log.e("ReminderVM", "Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("ReminderVM", "Error de red: ${e.message}")
            }
            isLoading = false
        }
    }
}