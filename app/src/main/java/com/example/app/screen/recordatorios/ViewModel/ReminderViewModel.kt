package com.example.app.screen.recordatorios.ViewModel

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReminderViewModel : ViewModel() {

    private var currentRingtone: Ringtone? = null
    private var playbackJob: Job? = null

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
}