package com.rutai.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import com.rutai.app.BaseViewModel
import com.rutai.app.models.ZonaPeligrosaResponse
import com.rutai.app.models.ZonaSugerida
import com.rutai.app.repository.RutasRepository
import com.rutai.app.utils.SessionManager

class ZonasSugeridasViewModel(context: Context) : BaseViewModel(context, SessionManager.getInstance(context)) {

    private val repository = RutasRepository()

    var zonasSugeridas by mutableStateOf<List<ZonaSugerida>>(emptyList())
        private set

    var mostrarSugerencias by mutableStateOf(false)
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
        errorMessage = null
        safeApiCall(
            call = { token -> repository.obtenerZonasSugeridas(token, lat, lon, radioKm) },
            onSuccess = { zonas ->
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
            },
            onError = { error ->
                errorMessage = error
                Log.e("ZonasSugeridas", "❌ Error: $error")
            }
        )
    }

    /**
     * 💾 Adopta (guarda) una zona sugerida como propia
     */
    fun adoptarZona(
        zonaId: Int,
        onSuccess: (ZonaPeligrosaResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        safeApiCall(
            call = { token -> repository.adoptarZonaSugerida(token, zonaId) },
            onSuccess = { zonaAdoptada ->
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
            },
            onError = { error ->
                onError(error)
                Log.e("ZonasSugeridas", "❌ Error: $error")
            }
        )
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
