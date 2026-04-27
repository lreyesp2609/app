package com.rutai.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutai.app.BuildConfig
import com.rutai.app.models.EstadoMensaje
import com.rutai.app.models.MensajeResponse
import com.rutai.app.models.MensajeUI
import com.rutai.app.network.ChatWebSocketListener
import com.rutai.app.websocket.WebSocketManager
import com.rutai.app.repository.MensajesRepository
import com.rutai.app.utils.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    init {
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

                    marcarTodoComoLeido(grupoId, mensajesUI)
                    conectarWebSocket(grupoId)
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Error desconocido"
                    Log.e(TAG, "❌ Error al cargar mensajes: ${exception.message}")
                }

            _isLoading.value = false
        }
    }

    private fun marcarTodoComoLeido(grupoId: Int, mensajes: List<MensajeUI>) {
        viewModelScope.launch {
            val mensajesNoLeidos = mensajes.filter { !it.esMio && !it.leido }

            if (mensajesNoLeidos.isEmpty()) {
                Log.d(TAG, "ℹ️ No hay mensajes no leídos que marcar")
                return@launch
            }

            Log.d(TAG, "👁️ Marcando ${mensajesNoLeidos.size} mensajes como leídos")

            mensajesNoLeidos.forEach { mensaje ->
                repository.marcarMensajeLeido(grupoId, mensaje.id)
                    .onSuccess {
                        Log.d(TAG, "✅ Mensaje ${mensaje.id} marcado como leído")
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "❌ Error al marcar mensaje ${mensaje.id}: ${exception.message}")
                    }
            }

            _mensajes.value = _mensajes.value.map { mensaje ->
                if (!mensaje.esMio && !mensaje.leido) {
                    mensaje.copy(leido = true)
                } else {
                    mensaje
                }
            }

            Log.d(TAG, "✅ Todos los mensajes marcados como leídos")
        }
    }

    private fun conectarWebSocket(grupoId: Int) {
        val token = sessionManager.getAccessToken() ?: run {
            Log.e(TAG, "❌ No hay token disponible para conectar WebSocket")
            return
        }

        val url = obtenerWebSocketUrl(grupoId, token)
        Log.d(TAG, "🔌 Conectando WebSocket al grupo $grupoId")

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
                                        val tempId = dataObject.get("temp_id")?.asString

                                        if (mensajeUI.esMio && tempId != null) {
                                            // 🔥 Reemplazar mensaje temporal por el real
                                            _mensajes.update { lista ->
                                                val indexTemporal = lista.indexOfFirst {
                                                    it.tempId == tempId
                                                }

                                                if (indexTemporal != -1) {
                                                    Log.d(TAG, "🔄 Reemplazando mensaje temporal $tempId por ID real ${mensajeUI.id}")
                                                    Log.d(TAG, "   Estado recibido: entregado=${mensajeUI.entregado}, leido_por=${mensajeUI.leidoPor}")

                                                    // 🆕 CALCULAR ESTADO CORRECTO BASADO EN LOS DATOS
                                                    val estadoCorrecto = when {
                                                        mensajeUI.leidoPor > 0 -> EstadoMensaje.LEIDO
                                                        mensajeUI.entregado -> EstadoMensaje.ENTREGADO
                                                        else -> EstadoMensaje.ENVIADO // ✅ ENVIADO (no entregado aún)
                                                    }

                                                    lista.toMutableList().apply {
                                                        set(indexTemporal, mensajeUI.copy(estado = estadoCorrecto))
                                                    }
                                                } else {
                                                    Log.w(TAG, "⚠️ No se encontró mensaje temporal con tempId=$tempId")
                                                    lista + mensajeUI
                                                }
                                            }
                                        } else if (!mensajeUI.esMio) {
                                            // Mensaje de otro usuario, agregar normalmente
                                            _mensajes.update { it + mensajeUI }
                                            Log.d(TAG, "💬 Nuevo mensaje de otro usuario: ID=${mensajeUI.id}")
                                            marcarComoLeido(grupoId, mensajeUI.id)
                                        }
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
                        "mensaje_entregado" -> {
                            val dataObject = jsonObject.getAsJsonObject("data")
                            val mensajeId = dataObject.get("mensaje_id")?.asInt
                            val entregado = dataObject.get("entregado")?.asBoolean ?: false

                            if (mensajeId != null) {
                                viewModelScope.launch {
                                    _mensajes.update { lista ->
                                        lista.map { mensaje ->
                                            if (mensaje.id == mensajeId) {
                                                Log.d(TAG, "📬 Mensaje $mensajeId actualizado: entregado=$entregado")
                                                mensaje.copy(
                                                    entregado = entregado,
                                                    estado = EstadoMensaje.ENTREGADO // 🆕 ACTUALIZAR ESTADO
                                                )
                                            } else mensaje
                                        }
                                    }
                                }
                            }
                        }

                        "mensaje_leido" -> {
                            val dataObject = jsonObject.getAsJsonObject("data")
                            val mensajeId = dataObject.get("mensaje_id")?.asInt
                            val leidoPor = dataObject.get("leido_por")?.asInt ?: 0

                            if (mensajeId != null) {
                                _mensajes.update { lista ->
                                    lista.map { msg ->
                                        if (msg.id == mensajeId) {
                                            Log.d(TAG, "👁️ Mensaje $mensajeId leído por $leidoPor personas")
                                            msg.copy(
                                                leido = true,
                                                leidoPor = leidoPor,
                                                estado = EstadoMensaje.LEIDO // 🆕 ACTUALIZAR ESTADO
                                            )
                                        } else msg
                                    }
                                }
                            }
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

    /**
     * 🆕 Envía un mensaje con ID temporal único
     */
    fun enviarMensaje(grupoId: Int, contenido: String) {
        if (contenido.isBlank()) {
            Log.w(TAG, "⚠️ Intento de enviar mensaje vacío")
            return
        }

        viewModelScope.launch {
            try {
                // 🆕 Generar ID temporal único
                val tempId = java.util.UUID.randomUUID().toString()

                // 🆕 Crear mensaje temporal para UI inmediata
                val mensajeTemporal = MensajeUI(
                    id = -1, // ID temporal negativo
                    tempId = tempId, // 🆕 Identificador único temporal
                    contenido = contenido,
                    esMio = true,
                    hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    entregado = false,
                    leido = false,
                    leidoPor = 0,
                    nombreRemitente = null,
                    remitenteId = currentUserId,
                    tipo = "texto",
                    fechaCreacion = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date()),
                    estado = EstadoMensaje.ENVIANDO // 🆕 Estado inicial
                )

                // 🆕 Agregar inmediatamente a la UI
                _mensajes.update { it + mensajeTemporal }
                Log.d(TAG, "✅ Mensaje agregado optimísticamente con tempId: $tempId")

                // 📤 Enviar por WebSocket con temp_id
                val mensajeEnvio = mapOf(
                    "action" to "mensaje",
                    "data" to mapOf(
                        "temp_id" to tempId, // 🆕 Incluir temp_id
                        "contenido" to contenido,
                        "tipo" to "texto"
                    )
                )

                val mensajeJson = Gson().toJson(mensajeEnvio)
                Log.d(TAG, "📤 Enviando mensaje con tempId=$tempId")

                val enviado = WebSocketManager.send(mensajeJson)

                if (!enviado) {
                    // 🆕 Marcar como error si no se pudo enviar
                    _mensajes.update { lista ->
                        lista.map { msg ->
                            if (msg.tempId == tempId) {
                                msg.copy(estado = EstadoMensaje.ERROR)
                            } else msg
                        }
                    }
                    _error.value = "Error al enviar mensaje"
                }

            } catch (e: Exception) {
                _error.value = "Error al enviar mensaje: ${e.message}"
                Log.e(TAG, "❌ Error al enviar mensaje: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun marcarComoLeido(grupoId: Int, mensajeId: Int) {
        viewModelScope.launch {
            val mensaje = _mensajes.value.find { it.id == mensajeId }

            if (mensaje?.esMio == true) {
                Log.d(TAG, "⚠️ Mensaje $mensajeId es mío, no se marca como leído")
                return@launch
            }

            Log.d(TAG, "👁️ Marcando mensaje $mensajeId como leído")
            repository.marcarMensajeLeido(grupoId, mensajeId)
                .onSuccess {
                    _mensajes.value = _mensajes.value.map { msg ->
                        if (msg.id == mensajeId) {
                            msg.copy(leido = true)
                        } else {
                            msg
                        }
                    }
                    Log.d(TAG, "✅ Mensaje $mensajeId marcado como leído")
                }
                .onFailure { exception ->
                    Log.e(TAG, "❌ Error al marcar como leído: ${exception.message}")
                }
        }
    }

    private fun obtenerWebSocketUrl(grupoId: Int, token: String): String {
        val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")

        val wsUrl = when {
            baseUrl.startsWith("https://") -> baseUrl.replaceFirst("https://", "wss://")
            baseUrl.startsWith("http://") -> baseUrl.replaceFirst("http://", "ws://")
            else -> baseUrl
        }

        return "$wsUrl/ws/$grupoId?token=$token"
    }

    fun limpiarError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 Limpiando ChatGrupoViewModel")
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

    // 🔥 CALCULAR ESTADO CORRECTO BASADO EN LOS DATOS
    val estadoMensaje = when {
        this.leidoPor != null && this.leidoPor > 0 -> EstadoMensaje.LEIDO
        this.entregado == true -> EstadoMensaje.ENTREGADO
        else -> EstadoMensaje.ENVIADO
    }

    return MensajeUI(
        id = this.id,
        tempId = null,
        contenido = this.contenido ?: "",
        esMio = this.remitenteId == currentUserId,
        hora = formatearHora(this.fechaCreacion ?: ""),
        entregado = this.entregado ?: false,
        leido = this.leido ?: false,
        leidoPor = this.leidoPor ?: 0,
        nombreRemitente = if (this.remitenteId != currentUserId) this.remitenteNombre else null,
        remitenteId = this.remitenteId,
        tipo = this.tipo ?: "texto",
        fechaCreacion = this.fechaCreacion ?: "",
        estado = estadoMensaje // 🆕 ESTADO CALCULADO CORRECTAMENTE
    )
}

private fun formatearHora(fechaISO: String): String {
    if (fechaISO.isBlank()) return "00:00"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val date = inputFormat.parse(fechaISO)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        Log.e("formatearHora", "Error al formatear hora: ${e.message}")
        "00:00"
    }
}