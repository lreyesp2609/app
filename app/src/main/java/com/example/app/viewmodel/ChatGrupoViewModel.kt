package com.example.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.models.MensajeResponse
import com.example.app.models.MensajeUI
import com.example.app.repository.MensajesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatGrupoViewModel(context: Context) : ViewModel() {

    private val repository = MensajesRepository(context)
    private val currentUserId = repository.getCurrentUserId()

    // Estados
    private val _mensajes = MutableStateFlow<List<MensajeUI>>(emptyList())
    val mensajes: StateFlow<List<MensajeUI>> = _mensajes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Carga los mensajes del grupo
     */
    fun cargarMensajes(grupoId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.obtenerMensajesGrupo(grupoId)
                .onSuccess { mensajesResponse ->
                    _mensajes.value = mensajesResponse.map { it.toMensajeUI(currentUserId) }
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error desconocido"
                }

            _isLoading.value = false
        }
    }

    /**
     * Marca un mensaje como leído
     */
    fun marcarComoLeido(grupoId: Int, mensajeId: Int) {
        viewModelScope.launch {
            repository.marcarMensajeLeido(grupoId, mensajeId)
                .onSuccess {
                    // Actualizar el estado local del mensaje
                    _mensajes.value = _mensajes.value.map { mensaje ->
                        if (mensaje.id == mensajeId) {
                            mensaje.copy(leido = true)
                        } else {
                            mensaje
                        }
                    }
                }
                .onFailure { exception ->
                    // Puedes manejar el error si lo deseas
                    println("Error al marcar como leído: ${exception.message}")
                }
        }
    }

    /**
     * Agrega un nuevo mensaje (para cuando envíes por WebSocket)
     */
    fun agregarMensaje(mensaje: MensajeUI) {
        _mensajes.value = _mensajes.value + mensaje
    }

    /**
     * Limpia el error
     */
    fun limpiarError() {
        _error.value = null
    }
}

/**
 * Extension function para convertir MensajeResponse a MensajeUI
 */
private fun MensajeResponse.toMensajeUI(currentUserId: Int): MensajeUI {
    return MensajeUI(
        id = this.id,
        contenido = this.contenido,
        esMio = this.remitenteId == currentUserId,
        hora = formatearHora(this.fechaCreacion),
        leido = this.leido,
        leidoPor = this.leidoPor,
        nombreRemitente = if (this.remitenteId != currentUserId) this.remitenteNombre else null,
        remitenteId = this.remitenteId,
        tipo = this.tipo,
        fechaCreacion = this.fechaCreacion
    )
}


/**
 * Formatea la fecha ISO 8601 a hora legible
 * Ejemplo: "2025-10-22T10:30:00" -> "10:30"
 */
private fun formatearHora(fechaISO: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = inputFormat.parse(fechaISO)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        "00:00"
    }
}