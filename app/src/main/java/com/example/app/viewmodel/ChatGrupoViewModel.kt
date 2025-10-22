package com.example.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.BuildConfig
import com.example.app.models.MensajeResponse
import com.example.app.models.MensajeUI
import com.example.app.network.ChatWebSocketListener
import com.example.app.network.WebSocketManager
import com.example.app.repository.MensajesRepository
import com.example.app.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatGrupoViewModel(context: Context) : ViewModel() {

    private val repository = MensajesRepository(context)
    private val sessionManager = SessionManager.getInstance(context)
    private val currentUserId = repository.getCurrentUserId()

    // Estados
    private val _mensajes = MutableStateFlow<List<MensajeUI>>(emptyList())
    val mensajes: StateFlow<List<MensajeUI>> = _mensajes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    companion object {
        private const val TAG = "🔌WS_ChatGrupoViewModel"
    }

    // ❌ ELIMINAR: Ya no necesitamos listener local
    // private val tokenChangeListener: (String) -> Unit = { ... }

    init {
        // ❌ ELIMINAR: Ya no registramos listener aquí
        // sessionManager.addTokenChangeListener(tokenChangeListener)
        Log.d(TAG, "🎬 ChatGrupoViewModel inicializado")
    }

    /**
     * Carga los mensajes del grupo y conecta al WebSocket
     */
    fun cargarMensajes(grupoId: Int) {
        viewModelScope.launch {
            Log.d(TAG, "📥 Cargando mensajes del grupo $grupoId")
            _isLoading.value = true
            _error.value = null

            repository.obtenerMensajesGrupo(grupoId)
                .onSuccess { mensajesResponse ->
                    val mensajesUI = mensajesResponse.mapNotNull { it.toMensajeUI(currentUserId) }
                    _mensajes.value = mensajesUI
                    Log.d(TAG, "✅ ${mensajesUI.size} mensajes cargados correctamente")
                    conectarWebSocket(grupoId)
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error desconocido"
                    Log.e(TAG, "❌ Error al cargar mensajes: ${exception.message}")
                }

            _isLoading.value = false
        }
    }

    /**
     * Conecta al WebSocket del grupo
     */
    private fun conectarWebSocket(grupoId: Int) {
        val token = sessionManager.getAccessToken() ?: run {
            Log.e(TAG, "❌ No hay token disponible para conectar WebSocket")
            return
        }

        val url = obtenerWebSocketUrl(grupoId, token)
        Log.d(TAG, "🔌 Conectando WebSocket al grupo $grupoId")
        Log.d(TAG, "   URL: $url")

        WebSocketManager.connect(url, ChatWebSocketListener(
            onMessageReceived = { mensajeJson ->
                try {
                    Log.v(TAG, "📩 JSON recibido: $mensajeJson")

                    val jsonObject = Gson().fromJson(mensajeJson, com.google.gson.JsonObject::class.java)
                    val type = jsonObject.get("type")?.asString

                    when (type) {
                        "system" -> {
                            val message = jsonObject.get("message")?.asString
                            Log.i(TAG, "ℹ️ Sistema: $message")
                        }
                        "warning" -> {
                            val message = jsonObject.get("message")?.asString
                            val code = jsonObject.get("code")?.asString
                            Log.w(TAG, "⚠️ Advertencia [$code]: $message")
                        }
                        "error" -> {
                            val message = jsonObject.get("message")?.asString
                            val code = jsonObject.get("code")?.asString
                            Log.e(TAG, "❌ Error [$code]: $message")
                            viewModelScope.launch {
                                _error.value = message ?: "Error del servidor"

                                if (code == "TOKEN_EXPIRED") {
                                    Log.e(TAG, "🔒 Token expirado, cerrando WebSocket")
                                    WebSocketManager.close()
                                }
                            }
                        }
                        "mensaje" -> {
                            val dataObject = jsonObject.getAsJsonObject("data")
                            if (dataObject != null) {
                                val mensaje = Gson().fromJson(dataObject, MensajeResponse::class.java)
                                val mensajeUI = mensaje.toMensajeUI(currentUserId)

                                if (mensajeUI != null) {
                                    viewModelScope.launch {
                                        _mensajes.value = _mensajes.value + mensajeUI
                                        Log.d(TAG, "💬 Nuevo mensaje agregado: ID=${mensajeUI.id}")
                                    }
                                } else {
                                    Log.w(TAG, "⚠️ Mensaje inválido ignorado: id=${mensaje.id}")
                                }
                            }
                        }
                        "pong" -> {
                            Log.v(TAG, "🏓 Pong recibido")
                        }
                        "token_refreshed" -> {
                            val message = jsonObject.get("message")?.asString
                            Log.i(TAG, "🔄 Token actualizado en backend: $message")
                        }
                        else -> {
                            Log.w(TAG, "⚠️ Tipo de mensaje desconocido: $type")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al parsear mensaje: ${e.message}")
                    e.printStackTrace()
                }
            },
            onConnected = {
                _isConnected.value = true
                Log.i(TAG, "✅ WebSocket conectado al grupo $grupoId")
            },
            onDisconnected = {
                _isConnected.value = false
                Log.w(TAG, "❌ WebSocket desconectado del grupo $grupoId")
            },
            onError = { error ->
                _error.value = "Error de conexión: $error"
                Log.e(TAG, "❌ Error en WebSocket: $error")
            }
        ))
    }

    // ❌ ELIMINAR: Ya no enviamos token desde aquí
    // private fun enviarNuevoToken(newToken: String) { ... }

    /**
     * Envía un mensaje a través del WebSocket
     */
    fun enviarMensaje(grupoId: Int, contenido: String) {
        if (contenido.isBlank()) {
            Log.w(TAG, "⚠️ Intento de enviar mensaje vacío")
            return
        }

        viewModelScope.launch {
            try {
                val mensajeEnvio = mapOf(
                    "action" to "mensaje",
                    "data" to mapOf(
                        "contenido" to contenido,
                        "tipo" to "texto"
                    )
                )

                val mensajeJson = Gson().toJson(mensajeEnvio)
                Log.d(TAG, "📤 Enviando mensaje: $contenido")
                WebSocketManager.send(mensajeJson)

            } catch (e: Exception) {
                _error.value = "Error al enviar mensaje: ${e.message}"
                Log.e(TAG, "❌ Error al enviar mensaje: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Marca un mensaje como leído
     */
    fun marcarComoLeido(grupoId: Int, mensajeId: Int) {
        viewModelScope.launch {
            Log.d(TAG, "👁️ Marcando mensaje $mensajeId como leído")
            repository.marcarMensajeLeido(grupoId, mensajeId)
                .onSuccess {
                    _mensajes.value = _mensajes.value.map { mensaje ->
                        if (mensaje.id == mensajeId) {
                            mensaje.copy(leido = true)
                        } else {
                            mensaje
                        }
                    }
                    Log.d(TAG, "✅ Mensaje $mensajeId marcado como leído")
                }
                .onFailure { exception ->
                    Log.e(TAG, "❌ Error al marcar como leído: ${exception.message}")
                }
        }
    }

    /**
     * Obtiene la URL del WebSocket
     */
    private fun obtenerWebSocketUrl(grupoId: Int, token: String): String {
        val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")

        val wsUrl = when {
            baseUrl.startsWith("https://") -> baseUrl.replaceFirst("https://", "wss://")
            baseUrl.startsWith("http://") -> baseUrl.replaceFirst("http://", "ws://")
            else -> baseUrl
        }

        return "$wsUrl/grupos/ws/grupos/$grupoId?token=$token"
    }

    /**
     * Limpia el error
     */
    fun limpiarError() {
        _error.value = null
    }

    /**
     * Desconecta el WebSocket al limpiar el ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 Limpiando ChatGrupoViewModel")
        // ❌ ELIMINAR: Ya no removemos listener
        // sessionManager.removeTokenChangeListener(tokenChangeListener)
        WebSocketManager.close()
        Log.d(TAG, "✅ WebSocket cerrado")
    }
}

/**
 * Extension function para convertir MensajeResponse a MensajeUI
 */
private fun MensajeResponse.toMensajeUI(currentUserId: Int): MensajeUI? {
    if (this.id == null || this.remitenteId == null) {
        Log.w("MensajeResponse", "⚠️ Mensaje sin ID o remitenteId, ignorando")
        return null
    }

    return MensajeUI(
        id = this.id,
        contenido = this.contenido ?: "",
        esMio = this.remitenteId == currentUserId,
        hora = formatearHora(this.fechaCreacion ?: ""),
        leido = this.leido ?: false,
        leidoPor = this.leidoPor ?: 0,
        nombreRemitente = if (this.remitenteId != currentUserId) this.remitenteNombre else null,
        remitenteId = this.remitenteId,
        tipo = this.tipo ?: "texto",
        fechaCreacion = this.fechaCreacion ?: ""
    )
}

/**
 * Formatea la fecha ISO 8601 a hora legible
 */
private fun formatearHora(fechaISO: String): String {
    if (fechaISO.isBlank()) return "00:00"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = inputFormat.parse(fechaISO)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        "00:00"
    }
}