package com.example.app.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.models.*
import com.example.app.network.RetrofitClient
import com.example.app.network.RetrofitInstance
import com.example.app.repository.RutasRepository
import kotlinx.coroutines.launch

class MapViewModel(
    private val rutasRepository: RutasRepository
) : ViewModel() {

    private val _route = mutableStateOf<DirectionsResponse?>(null)
    val route: State<DirectionsResponse?> = _route

    private var currentMode = "driving-car" // foot-walking, cycling-regular, etc.
    private var currentMLType: String? = null
    private var currentToken: String? = null

    private val _rutaIdActiva = mutableStateOf<Int?>(null)
    val rutaIdActiva: State<Int?> = _rutaIdActiva

    private val _mostrarOpcionesFinalizar = mutableStateOf(false)
    val mostrarOpcionesFinalizar: State<Boolean> = _mostrarOpcionesFinalizar

    // 🔥 NUEVAS VARIABLES para guardar datos de la ruta actual
    private var rutaActualUbicacionId: Int? = null
    private var rutaActualDistancia: Double? = null
    private var rutaActualDuracion: Double? = null

    fun setMode(mode: String) {
        currentMode = mode
    }

    fun setToken(token: String) {
        currentToken = token
    }

    // Función principal con ML y autenticación
    fun fetchRouteWithML(
        start: Pair<Double, Double>,
        end: Pair<Double, Double>,
        token: String,
        ubicacionId: Int,
        transporteTexto: String? = null
    ) {
        currentToken = token
        rutaActualUbicacionId = ubicacionId // 🔥 GUARDAR para el feedback

        viewModelScope.launch {
            try {
                // PASO 1: Obtener recomendación ML
                Log.d("MapViewModel", "Consultando ML con token...")

                val recomendacion = RetrofitClient.mlService.getRecomendacionTipoRuta(
                    "Bearer $token",
                    TipoRutaRequest(ubicacionId)
                )

                currentMLType = recomendacion.tipo_ruta
                Log.d("MapViewModel", "ML recomienda: ${recomendacion.tipo_ruta} para usuario ${recomendacion.usuario_id}")

                // PASO 2: Configurar OpenRouteService según ML
                val (preference, avoidOptions) = recomendacion.tipo_ruta.toORSConfig()

                val request = DirectionsRequest(
                    coordinates = listOf(
                        listOf(start.second, start.first),
                        listOf(end.second, end.first)
                    ),
                    preference = preference,
                    options = avoidOptions
                )

                Log.d("MapViewModel", "Enviando request ORS: $request")
                Log.d("MapViewModel", "🔥 USANDO TIPO ML: ${recomendacion.tipo_ruta} -> ORS preference: $preference")

                // PASO 3: Obtener ruta de OpenRouteService
                val response = RetrofitInstance.api.getRoute(currentMode, request)
                val responseWithProfile = response.copy(profile = currentMode)

                // 🔥 GUARDAR datos de la ruta para el feedback
                response.routes.firstOrNull()?.summary?.let { summary ->
                    rutaActualDistancia = summary.distance.toDouble()
                    rutaActualDuracion = summary.duration
                }

                Log.d("MapViewModel", "Ruta obtenida: distancia=${response.routes.firstOrNull()?.summary?.distance}m")

                _route.value = responseWithProfile

                // PASO 4: Guardar ruta CON el tipo usado
                if (transporteTexto != null) {
                    guardarRutaEnBackend(
                        responseWithProfile,
                        token,
                        ubicacionId,
                        transporteTexto,
                        recomendacion.tipo_ruta
                    )
                }

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error fetching route with ML", e)
                _route.value = null
            }
        }
    }

    private suspend fun guardarRutaEnBackend(
        response: DirectionsResponse,
        token: String,
        ubicacionId: Int,
        transporteTexto: String,
        tipoRutaUsado: String
    ) {
        try {
            val rutaJson = response.toRutaUsuarioJson(
                ubicacionId = ubicacionId,
                transporteTexto = transporteTexto,
                tipoRutaUsado = tipoRutaUsado
            )

            Log.d("MapViewModel", "Guardando ruta con tipo ML: $tipoRutaUsado")

            val result = rutasRepository.guardarRuta(token, rutaJson)
            result.onSuccess { rutaGuardada ->
                Log.d("MapViewModel", "✅ Ruta guardada correctamente con tipo: $tipoRutaUsado")
                Log.d("MapViewModel", "Ruta completa: $rutaGuardada")
                _rutaIdActiva.value = rutaGuardada.id
                _mostrarOpcionesFinalizar.value = true
            }.onFailure { error ->
                Log.e("MapViewModel", "❌ Error al guardar ruta: ${error.message}")
            }
        } catch (e: Exception) {
            Log.e("MapViewModel", "Excepción al guardar ruta", e)
        }
    }

    fun clearRoute() {
        _route.value = null
        currentMLType = null
        // 🔥 LIMPIAR datos de feedback
        rutaActualUbicacionId = null
        rutaActualDistancia = null
        rutaActualDuracion = null
    }

    // 🔥 FUNCIÓN FINALIZAR - SIN FEEDBACK DUPLICADO
    fun finalizarRutaBackend(rutaId: Int) {
        viewModelScope.launch {
            try {
                // Solo finalizar la ruta - el backend ya maneja el feedback UCB
                rutasRepository.finalizarRuta(rutaId)
                Log.d("MapViewModel", "✅ Ruta finalizada en backend (feedback automático)")

                // Limpiar estado
                _mostrarOpcionesFinalizar.value = false
                _route.value = null

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error finalizando ruta", e)
            }
        }
    }

    // 🔥 FUNCIÓN CANCELAR - SIN FEEDBACK DUPLICADO
    fun cancelarRutaBackend(rutaId: Int) {
        viewModelScope.launch {
            try {
                // Solo cancelar la ruta - el backend ya maneja el feedback UCB
                rutasRepository.cancelarRuta(rutaId)
                Log.d("MapViewModel", "✅ Ruta cancelada en backend (feedback automático)")

                // Limpiar estado
                _mostrarOpcionesFinalizar.value = false
                _route.value = null

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error cancelando ruta", e)
            }
        }
    }

    // 🔥 NUEVA FUNCIÓN para enviar feedback al UCB
    private suspend fun enviarFeedbackUCB(completada: Boolean) {
        try {
            // Verificar que tenemos todos los datos necesarios
            val token = currentToken
            val tipoUsado = currentMLType
            val ubicacionId = rutaActualUbicacionId
            val distancia = rutaActualDistancia
            val duracion = rutaActualDuracion

            if (token == null || tipoUsado == null || ubicacionId == null) {
                Log.w("MapViewModel", "❌ Faltan datos para enviar feedback UCB")
                return
            }

            // Crear el feedback request
            val feedbackRequest = FeedbackRequest(
                tipo_usado = tipoUsado,
                completada = completada,
                ubicacion_id = ubicacionId,
                distancia = distancia,
                duracion = duracion
            )

            Log.d("MapViewModel", "📤 Enviando feedback UCB: $feedbackRequest")

            // Enviar al endpoint de ML
            val response = RetrofitClient.mlService.enviarFeedback(
                "Bearer $token",
                feedbackRequest
            )

            Log.d("MapViewModel", "✅ Feedback UCB enviado: ${response.mensaje}")

        } catch (e: Exception) {
            Log.e("MapViewModel", "❌ Error enviando feedback UCB: ${e.message}", e)
        }
    }
}