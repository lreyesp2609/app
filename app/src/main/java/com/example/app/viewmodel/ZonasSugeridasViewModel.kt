package com.example.app.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.util.Log
import com.example.app.models.ZonaPeligrosaResponse
import com.example.app.models.ZonaSugerida
import com.example.app.network.RetrofitClient

class ZonasSugeridasViewModel(private val token: String) : ViewModel() {

    var zonasSugeridas by mutableStateOf<List<ZonaSugerida>>(emptyList())
        private set

    var mostrarSugerencias by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * 🔍 Verifica si hay zonas sugeridas cerca de la ubicación actual
     */
    fun verificarYCargarSugerencias(
        lat: Double,
        lon: Double,
        radioKm: Float = 10.0f
    ) {
        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null

                Log.d("ZonasSugeridas", "🔍 Buscando zonas cerca de ($lat, $lon) en radio ${radioKm}km")

                val zonas = RetrofitClient.rutasApiService.obtenerZonasSugeridas(
                    token = "Bearer $token",
                    lat = lat,
                    lon = lon,
                    radioKm = radioKm
                )

                if (zonas.isNotEmpty()) {
                    // Calcular distancia a cada zona
                    zonasSugeridas = zonas.map { zona ->
                        val centro = zona.poligono.firstOrNull()
                        val distancia = if (centro != null) {
                            calcularDistanciaKm(lat, lon, centro.lat, centro.lon)
                        } else {
                            0f
                        }

                        ZonaSugerida(
                            zonaOriginal = zona,
                            distanciaKm = distancia
                        )
                    }.sortedBy { it.distanciaKm } // Más cercanas primero

                    mostrarSugerencias = true

                    Log.d("ZonasSugeridas", "✅ ${zonas.size} zonas sugeridas encontradas")
                } else {
                    mostrarSugerencias = false
                    Log.d("ZonasSugeridas", "ℹ️ No hay zonas sugeridas en esta área")
                }

            } catch (e: Exception) {
                errorMessage = "Error al cargar zonas sugeridas: ${e.message}"
                Log.e("ZonasSugeridas", "❌ Error: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * 💾 Adopta (guarda) una zona sugerida como propia
     */
    fun adoptarZona(
        zonaId: Int,
        onSuccess: (ZonaPeligrosaResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                isLoading = true

                val zonaAdoptada = RetrofitClient.rutasApiService.adoptarZonaSugerida(
                    token = "Bearer $token",
                    zonaId = zonaId
                )

                // Marcar como adoptada en la lista
                zonasSugeridas = zonasSugeridas.map {
                    if (it.zonaOriginal.id == zonaId) {
                        it.copy(yaAdoptada = true)
                    } else {
                        it
                    }
                }

                onSuccess(zonaAdoptada)
                Log.d("ZonasSugeridas", "✅ Zona $zonaId adoptada correctamente")

            } catch (e: Exception) {
                val error = when {
                    e.message?.contains("Ya tienes una zona") == true ->
                        "Ya tienes una zona con ese nombre"
                    else ->
                        "Error al adoptar zona: ${e.message}"
                }
                onError(error)
                Log.e("ZonasSugeridas", "❌ Error: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * ❌ Descarta una zona (la quita de la lista temporal)
     */
    fun descartarZona(zonaId: Int) {
        zonasSugeridas = zonasSugeridas.filter {
            it.zonaOriginal.id != zonaId
        }

        if (zonasSugeridas.isEmpty()) {
            mostrarSugerencias = false
        }

        Log.d("ZonasSugeridas", "🗑️ Zona $zonaId descartada")
    }

    /**
     * 🔄 Resetea el estado
     */
    fun reset() {
        zonasSugeridas = emptyList()
        mostrarSugerencias = false
        errorMessage = null
    }

    // Helper para calcular distancia
    private fun calcularDistanciaKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val R = 6371.0 // Radio de la Tierra en km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return (R * c).toFloat()
    }
}