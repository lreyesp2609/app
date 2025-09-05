package com.example.app.viewmodels

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
        ubicacionId: Int? = null,
        transporteTexto: String? = null
    ) {
        currentToken = token

        viewModelScope.launch {
            try {
                // PASO 1: Obtener recomendación ML con JWT
                Log.d("MapViewModel", "Consultando ML con token...")

                val recomendacion = try {
                    RetrofitClient.mlService.getRecomendacionTipoRuta("Bearer $token")
                } catch (e: Exception) {
                    Log.w("MapViewModel", "Error en ML, usando fallback: ${e.message}")
                    TipoRutaResponse("fastest", 0) // Fallback
                }

                currentMLType = recomendacion.tipo_ruta
                Log.d("MapViewModel", "ML recomienda: ${recomendacion.tipo_ruta} para usuario ${recomendacion.usuario_id}")

                // PASO 2: Configurar OpenRouteService según ML
                val (preference, avoidOptions) = recomendacion.tipo_ruta.toORSConfig()

                val request = DirectionsRequest(
                    coordinates = listOf(
                        listOf(start.second, start.first), // lon, lat
                        listOf(end.second, end.first)
                    ),
                    preference = preference,
                    options = avoidOptions
                )

                Log.d("MapViewModel", "Enviando request ORS: $request")

                // PASO 3: Obtener ruta de OpenRouteService
                val response = RetrofitInstance.api.getRoute(currentMode, request)
                val responseWithProfile = response.copy(profile = currentMode)

                Log.d("MapViewModel", "Ruta obtenida: distancia=${response.routes.firstOrNull()?.summary?.distance}m")

                _route.value = responseWithProfile

                // PASO 4: Guardar ruta (si se necesita)
                if (ubicacionId != null && transporteTexto != null) {
                    guardarRutaEnBackend(responseWithProfile, token, ubicacionId, transporteTexto)
                }

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error fetching route with ML", e)
                _route.value = null
            }
        }
    }

    // Función para reportar resultado con JWT
    fun reportarResultadoNavegacion(
        completada: Boolean,
        distancia: Double? = null,
        duracion: Double? = null
    ) {
        viewModelScope.launch {
            try {
                if (currentMLType != null && currentToken != null) {
                    val feedback = FeedbackRequest(
                        tipo_usado = currentMLType!!,
                        completada = completada,
                        distancia = distancia,
                        duracion = duracion
                    )

                    val result = RetrofitClient.mlService.enviarFeedback("Bearer $currentToken", feedback)
                    Log.d("MapViewModel", "Feedback enviado: ${result.mensaje}")

                    currentMLType = null // Reset
                }
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error enviando feedback ML", e)
            }
        }
    }

    // Función para obtener estadísticas del usuario
    fun obtenerEstadisticasML(token: String) {
        viewModelScope.launch {
            try {
                val stats = RetrofitClient.mlService.obtenerMisEstadisticas("Bearer $token")
                Log.d("MapViewModel", "Estadísticas ML: $stats")
                // Puedes agregar un StateFlow para mostrar estas stats en UI
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error obteniendo estadísticas ML", e)
            }
        }
    }

    private suspend fun guardarRutaEnBackend(
        response: DirectionsResponse,
        token: String,
        ubicacionId: Int,
        transporteTexto: String
    ) {
        try {
            val rutaJson = response.toRutaUsuarioJson(
                ubicacionId = ubicacionId,
                transporteTexto = transporteTexto
            )

            Log.d("MapViewModel", "Guardando ruta...")

            val result = rutasRepository.guardarRuta(token, rutaJson)
            result.onSuccess { rutaGuardada ->
                Log.d("MapViewModel", "Ruta guardada correctamente: $rutaGuardada")
                _rutaIdActiva.value = rutaGuardada.id   // <-- necesitas un State<Int?>
                _mostrarOpcionesFinalizar.value = true // <-- necesitas un State<Boolean>
            }.onFailure { error ->
                Log.e("MapViewModel", "Error al guardar ruta: ${error.message}")
            }
        } catch (e: Exception) {
            Log.e("MapViewModel", "Excepción al guardar ruta", e)
        }
    }

    fun clearRoute() {
        _route.value = null
        currentMLType = null
    }

    // Función simple sin ML (para casos de fallback)
    fun fetchSimpleRoute(
        start: Pair<Double, Double>,
        end: Pair<Double, Double>
    ) {
        viewModelScope.launch {
            try {
                val request = DirectionsRequest(
                    coordinates = listOf(
                        listOf(start.second, start.first),
                        listOf(end.second, end.first)
                    )
                )

                val response = RetrofitInstance.api.getRoute(currentMode, request)
                _route.value = response.copy(profile = currentMode)

                Log.d("MapViewModel", "Ruta simple obtenida")
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error fetching simple route", e)
                _route.value = null
            }
        }
    }

    fun finalizarRutaBackend(rutaId: Int) {
        viewModelScope.launch {
            try {
                rutasRepository.finalizarRuta(rutaId) // Llama al endpoint PATCH /rutas/{id}/finalizar
                _mostrarOpcionesFinalizar.value = false
                _route.value = null
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error finalizando ruta", e)
            }
        }
    }

    fun cancelarRutaBackend(rutaId: Int) {
        viewModelScope.launch {
            try {
                rutasRepository.cancelarRuta(rutaId)
                _mostrarOpcionesFinalizar.value = false
                _route.value = null
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error cancelando ruta", e)
            }
        }
    }
}
