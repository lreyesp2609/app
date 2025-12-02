package com.example.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.models.MiembroUbicacion
import com.example.app.services.LocationTrackingService
import com.example.app.utils.SessionManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationGrupoViewModel(context: Context) : ViewModel() {

    private val sessionManager = SessionManager.getInstance(context)
    private val currentUserId = sessionManager.getUser()?.id ?: 0

    // ✅ Guardar el grupoId actual
    private var currentGrupoId: Int? = null

    // Estados
    private val _ubicacionesMiembros = MutableStateFlow<List<MiembroUbicacion>>(emptyList())
    val ubicacionesMiembros: StateFlow<List<MiembroUbicacion>> = _ubicacionesMiembros.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val messageListener: (String) -> Unit = { message ->
        handleWebSocketMessage(message)
    }

    companion object {
        private const val TAG = "📍WS_SessionManager"
    }

    init {
        Log.d(TAG, "🎬 LocationGrupoViewModel inicializado")
    }

    fun suscribirseAUbicaciones(grupoId: Int) {
        Log.d(TAG, "📢 Suscribiéndose al grupo $grupoId")
        currentGrupoId = grupoId // ✅ Guardar el ID
        LocationTrackingService.addMessageListener(grupoId, messageListener)
        _isConnected.value = true
    }

    private fun handleWebSocketMessage(ubicacionJson: String) {
        try {
            Log.v(TAG, "📍 JSON recibido: ${ubicacionJson.take(100)}")

            val jsonObject = Gson().fromJson(ubicacionJson, JsonObject::class.java)
            val type = jsonObject.get("type")?.asString

            when (type) {
                "ubicaciones_iniciales" -> {
                    val ubicacionesArray = jsonObject.getAsJsonArray("ubicaciones")
                    val lista = mutableListOf<MiembroUbicacion>()

                    ubicacionesArray?.forEach { element ->
                        val ub = element.asJsonObject
                        val userId = ub.get("user_id")?.asInt ?: return@forEach

                        if (userId != currentUserId) {
                            lista.add(
                                MiembroUbicacion(
                                    usuarioId = userId,
                                    nombre = ub.get("nombre")?.asString ?: "Usuario",
                                    lat = ub.get("lat")?.asDouble ?: 0.0,
                                    lon = ub.get("lon")?.asDouble ?: 0.0,
                                    timestamp = ub.get("timestamp")?.asString ?: "",
                                    esCreador = ub.get("es_creador")?.asBoolean ?: false
                                )
                            )
                        }
                    }

                    viewModelScope.launch {
                        _ubicacionesMiembros.value = lista
                        Log.d(TAG, "📍 ${lista.size} ubicaciones iniciales recibidas")
                    }
                }

                "ubicacion_update" -> {
                    val userId = jsonObject.get("user_id")?.asInt
                    if (userId == null || userId == currentUserId) {
                        return
                    }

                    val nombre = jsonObject.get("nombre")?.asString ?: "Usuario"
                    val lat = jsonObject.get("lat")?.asDouble ?: 0.0
                    val lon = jsonObject.get("lon")?.asDouble ?: 0.0
                    val timestamp = jsonObject.get("timestamp")?.asString ?: ""
                    val esCreador = jsonObject.get("es_creador")?.asBoolean ?: false

                    val nuevaUbicacion = MiembroUbicacion(
                        usuarioId = userId,
                        nombre = nombre,
                        lat = lat,
                        lon = lon,
                        timestamp = timestamp,
                        esCreador = esCreador,
                        activo = true
                    )

                    viewModelScope.launch {
                        val current = _ubicacionesMiembros.value.toMutableList()
                        val index = current.indexOfFirst { it.usuarioId == userId }

                        if (index >= 0) {
                            current[index] = nuevaUbicacion
                            Log.d(TAG, "🔄 Ubicación actualizada: $nombre")
                        } else {
                            current.add(nuevaUbicacion)
                            Log.d(TAG, "➕ Nueva ubicación: $nombre")
                        }

                        _ubicacionesMiembros.value = current
                        Log.d(TAG, "📊 Total miembros: ${current.size}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al parsear: ${e.message}")
            e.printStackTrace()
        }
    }

    // ✅ Usar el grupoId guardado
    fun desuscribirse() {
        currentGrupoId?.let { grupoId ->
            Log.d(TAG, "📢 Desuscribiéndose del grupo $grupoId")
            LocationTrackingService.removeMessageListener(grupoId, messageListener)
            _isConnected.value = false
            currentGrupoId = null
        }
    }

    fun limpiarError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 Limpiando LocationGrupoViewModel")
        desuscribirse() // ✅ Ahora no necesita parámetro
    }
}