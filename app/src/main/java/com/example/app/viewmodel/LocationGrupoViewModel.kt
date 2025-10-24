package com.example.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.BuildConfig
import com.example.app.models.MiembroUbicacion
import com.example.app.network.WebSocketLocationManager
import com.example.app.services.LocationWebSocketListener
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

    // Estados
    private val _ubicacionesMiembros = MutableStateFlow<List<MiembroUbicacion>>(emptyList())
    val ubicacionesMiembros: StateFlow<List<MiembroUbicacion>> = _ubicacionesMiembros.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 🆕 Listener del ViewModel
    private var viewModelListener: LocationWebSocketListener? = null

    companion object {
        private const val TAG = "📍WS_SessionManager"
    }

    init {
        Log.d(TAG, "🎬 LocationGrupoViewModel inicializado")
    }

    /**
     * 🆕 Se SUSCRIBE al WebSocket existente en lugar de crear uno nuevo
     */
    fun suscribirseAUbicaciones() {
        Log.d(TAG, "📢 Suscribiéndose al WebSocket de ubicaciones")

        viewModelListener = LocationWebSocketListener(
            onUbicacionRecibida = lambda@{ ubicacionJson ->
                try {
                    Log.v(TAG, "📍 JSON recibido en ViewModel: ${ubicacionJson.take(100)}")

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
                                            userId = userId,
                                            nombre = ub.get("nombre")?.asString ?: "Usuario",
                                            lat = ub.get("lat")?.asDouble ?: 0.0,
                                            lon = ub.get("lon")?.asDouble ?: 0.0,
                                            timestamp = ub.get("timestamp")?.asString ?: ""
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
                                return@lambda
                            }

                            val nombre = jsonObject.get("nombre")?.asString ?: "Usuario"
                            val lat = jsonObject.get("lat")?.asDouble ?: 0.0
                            val lon = jsonObject.get("lon")?.asDouble ?: 0.0
                            val timestamp = jsonObject.get("timestamp")?.asString ?: ""

                            val nuevaUbicacion = MiembroUbicacion(userId, nombre, lat, lon, timestamp)

                            viewModelScope.launch {
                                val current = _ubicacionesMiembros.value.toMutableList()
                                val index = current.indexOfFirst { it.userId == userId }

                                if (index >= 0) {
                                    current[index] = nuevaUbicacion
                                    Log.d(TAG, "🔄 Ubicación actualizada: $nombre en ($lat, $lon)")
                                } else {
                                    current.add(nuevaUbicacion)
                                    Log.d(TAG, "➕ Nueva ubicación agregada: $nombre en ($lat, $lon)")
                                }

                                _ubicacionesMiembros.value = current
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al parsear mensaje: ${e.message}")
                    e.printStackTrace()
                }
            },
            onConnected = {
                _isConnected.value = true
                Log.i(TAG, "✅ ViewModel conectado al WebSocket")
            },
            onDisconnected = {
                _isConnected.value = false
                Log.w(TAG, "❌ ViewModel desconectado del WebSocket")
            },
            onError = { error ->
                _error.value = "Error de conexión: $error"
                Log.e(TAG, "❌ Error en WebSocket: $error")
            }
        )

        // 📢 Agregar al broadcast en lugar de conectar
        viewModelListener?.let { WebSocketLocationManager.addBroadcastListener(it) }
    }

    fun limpiarError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 Limpiando LocationGrupoViewModel")

        // 🆕 SOLO desuscribirse, NO cerrar el WebSocket
        viewModelListener?.let {
            WebSocketLocationManager.removeBroadcastListener(it)
            Log.d(TAG, "📢 ViewModel desuscrito del WebSocket")
        }
    }
}