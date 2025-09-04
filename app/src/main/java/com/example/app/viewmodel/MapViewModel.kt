import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.runtime.State
import com.example.app.models.DirectionsRequest
import com.example.app.models.DirectionsResponse
import com.example.app.models.RutaUsuario
import com.example.app.models.toRutaUsuarioJson
import com.example.app.network.RetrofitInstance
import com.example.app.repository.RutasRepository

class MapViewModel(
    private val rutasRepository: RutasRepository // inyectar tu repo
) : ViewModel() {

    private val _route = mutableStateOf<DirectionsResponse?>(null)
    val route: State<DirectionsResponse?> = _route

    private var currentMode = "foot-walking"  // valor por defecto

    fun setMode(mode: String) {
        currentMode = mode
    }

    fun fetchRoute(start: Pair<Double, Double>, end: Pair<Double, Double>, token: String? = null, ubicacionId: Int? = null, transporteTexto: String? = null) {
        viewModelScope.launch {
            try {
                val profile = currentMode

                val request = DirectionsRequest(
                    coordinates = listOf(
                        listOf(start.second, start.first), // lon, lat
                        listOf(end.second, end.first)
                    )
                )

                Log.d("MapViewModel", "Enviando request: $request")
                println("Enviando request: $request")

                val response = RetrofitInstance.api.getRoute(profile, request)
                val responseWithProfile = response.copy(profile = currentMode)

                Log.d("MapViewModel", "Recibiendo response: $responseWithProfile")
                println("Recibiendo response: $responseWithProfile")

                _route.value = responseWithProfile

                // Ahora guardar la ruta si se proporcionaron los parámetros - DIRECTAMENTE aquí
                if (token != null && ubicacionId != null && transporteTexto != null) {
                    try {
                        val rutaJson = responseWithProfile.toRutaUsuarioJson(
                            ubicacionId = ubicacionId,
                            transporteTexto = transporteTexto
                        )

                        Log.d("MapViewModel", "Guardando ruta...")

                        val result = rutasRepository.guardarRuta(token, rutaJson)
                        result.onSuccess { rutaGuardada ->
                            Log.d("MapViewModel", "Ruta guardada correctamente: $rutaGuardada")
                        }.onFailure { error ->
                            Log.e("MapViewModel", "Error al guardar ruta: ${error.message}")
                        }
                    } catch (e: Exception) {
                        Log.e("MapViewModel", "Excepción al guardar ruta", e)
                    }
                }

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error fetching route", e)
                println("Error fetching route: ${e.message}")
                _route.value = null
            }
        }
    }

    fun clearRoute() {
        _route.value = null
    }

    // Función pública para guardar ruta cuando ya existe una ruta calculada
    fun guardarRuta(token: String, ubicacionId: Int, transporteTexto: String) {
        val currentRoute = _route.value
        if (currentRoute == null) {
            Log.w("MapViewModel", "No hay ruta calculada para enviar")
            return
        }

        viewModelScope.launch {
            try {
                val rutaJson = currentRoute.toRutaUsuarioJson(
                    ubicacionId = ubicacionId,
                    transporteTexto = transporteTexto
                )

                Log.d("MapViewModel", "Guardando ruta...")

                val result = rutasRepository.guardarRuta(token, rutaJson)
                result.onSuccess { rutaGuardada ->
                    Log.d("MapViewModel", "Ruta guardada correctamente: $rutaGuardada")
                }.onFailure { error ->
                    Log.e("MapViewModel", "Error al guardar ruta: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e("MapViewModel", "Excepción al guardar ruta", e)
            }
        }
    }
}