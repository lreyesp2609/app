package com.example.app.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton que mantiene la ubicación del usuario en memoria
 * y la comparte entre todas las pantallas de la app
 */
class LocationManager private constructor() {

    companion object {
        @Volatile
        private var instance: LocationManager? = null

        fun getInstance(): LocationManager {
            return instance ?: synchronized(this) {
                instance ?: LocationManager().also { instance = it }
            }
        }
    }

    // Estado de la ubicación
    private val _locationState = MutableStateFlow<LocationState>(LocationState.Loading)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    // Última ubicación conocida
    private var lastKnownLocation: UserLocation? = null

    /**
     * Actualiza la ubicación del usuario
     */
    fun updateLocation(lat: Double, lon: Double, address: String = "") {
        lastKnownLocation = UserLocation(lat, lon, address, System.currentTimeMillis())
        _locationState.value = LocationState.Success(lastKnownLocation!!)
        Log.d("LocationManager", "✅ Ubicación actualizada: $lat, $lon")
    }

    /**
     * Obtiene la última ubicación conocida
     * @param maxAgeMillis Edad máxima de la ubicación en milisegundos (default: 5 minutos)
     * @return UserLocation si existe y no es muy antigua, null en caso contrario
     */
    fun getLastKnownLocation(maxAgeMillis: Long = 5 * 60 * 1000): UserLocation? {
        val location = lastKnownLocation ?: return null
        val age = System.currentTimeMillis() - location.timestamp

        return if (age <= maxAgeMillis) {
            Log.d("LocationManager", "✅ Usando ubicación en caché (${age / 1000}s de antigüedad)")
            location
        } else {
            Log.d("LocationManager", "⚠️ Ubicación en caché muy antigua (${age / 1000}s)")
            null
        }
    }

    /**
     * Verifica si tenemos una ubicación válida
     */
    fun hasValidLocation(maxAgeMillis: Long = 5 * 60 * 1000): Boolean {
        return getLastKnownLocation(maxAgeMillis) != null
    }

    /**
     * Marca la ubicación como cargando
     */
    fun setLoading() {
        _locationState.value = LocationState.Loading
    }

    /**
     * Marca un error en la ubicación
     */
    fun setError(message: String) {
        _locationState.value = LocationState.Error(message)
        Log.e("LocationManager", "❌ Error de ubicación: $message")
    }

    /**
     * Limpia la ubicación guardada
     */
    fun clearLocation() {
        lastKnownLocation = null
        _locationState.value = LocationState.Loading
        Log.d("LocationManager", "🗑️ Ubicación limpiada")
    }
}

/**
 * Data class que representa la ubicación del usuario
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Estados posibles de la ubicación
 */
sealed class LocationState {
    object Loading : LocationState()
    data class Success(val location: UserLocation) : LocationState()
    data class Error(val message: String) : LocationState()
}