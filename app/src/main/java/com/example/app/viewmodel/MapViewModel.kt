import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.util.Log

import androidx.compose.runtime.State
import com.example.app.models.DirectionsRequest
import com.example.app.models.DirectionsResponse
import com.example.app.network.RetrofitInstance

class MapViewModel : ViewModel() {

    private val _route = mutableStateOf<DirectionsResponse?>(null)
    val route: State<DirectionsResponse?> = _route

    private var currentMode = "walking"

    fun setMode(mode: String) {
        currentMode = mode
    }

    fun fetchRoute(start: Pair<Double, Double>, end: Pair<Double, Double>) {
        viewModelScope.launch {
            try {
                val profile = when (currentMode) {
                    "walking" -> "foot-walking"
                    "cycling" -> "cycling-regular"
                    "driving" -> "driving-car"
                    else -> "foot-walking"
                }

                val request = DirectionsRequest(
                    coordinates = listOf(
                        listOf(start.second, start.first), // lon, lat
                        listOf(end.second, end.first)
                    )
                )

                // Mostrar request en consola
                Log.d("MapViewModel", "Enviando request: $request")
                println("Enviando request: $request")

                val response = RetrofitInstance.api.getRoute(profile, request)

                // Agregar profile al response para visualizar en la app
                val responseWithProfile = response.copy(profile = currentMode)

                // Mostrar response en consola
                Log.d("MapViewModel", "Recibiendo response: $responseWithProfile")
                println("Recibiendo response: $responseWithProfile")

                _route.value = responseWithProfile

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
}