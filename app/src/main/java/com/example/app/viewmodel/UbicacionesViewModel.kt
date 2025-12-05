package com.example.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.models.UbicacionUsuarioCreate
import com.example.app.models.UbicacionUsuarioResponse
import com.example.app.repository.UbicacionesRepository
import kotlinx.coroutines.launch

class UbicacionesViewModel(private val token: String) : ViewModel() {

    private val repository = UbicacionesRepository()

    var ubicaciones by mutableStateOf<List<UbicacionUsuarioResponse>>(emptyList())
        private set

    var ubicacionSeleccionada by mutableStateOf<UbicacionUsuarioResponse?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun crearUbicacion(
        ubicacion: UbicacionUsuarioCreate,
        callback: (success: Boolean, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true

            repository.crearUbicacion(token, ubicacion).fold(
                onSuccess = { nuevaUbicacion ->
                    ubicaciones = ubicaciones + nuevaUbicacion
                    isLoading = false
                    callback(true, null)
                },
                onFailure = { exception ->
                    isLoading = false

                    val error = when {
                        exception.message?.contains("LOCATION_NAME_ALREADY_EXISTS") == true ->
                            "Ya tienes una ubicación con este nombre"

                        exception.message?.contains("NETWORK_ERROR") == true ->
                            "Error de conexión. Verifica tu internet"

                        exception.message?.contains("HTTP_ERROR") == true ->
                            "Error de comunicación con el servidor"

                        else ->
                            "Error al crear la ubicación"
                    }

                    callback(false, error)
                }
            )
        }
    }

    fun cargarUbicaciones() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            repository.obtenerUbicaciones(token).fold(
                onSuccess = {
                    ubicaciones = it
                    isLoading = false
                },
                onFailure = {
                    isLoading = false
                    errorMessage = "No se pudieron cargar las ubicaciones"
                }
            )
        }
    }

    fun cargarUbicacionPorId(id: Int) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            repository.obtenerUbicacionPorId(token, id).fold(
                onSuccess = {
                    ubicacionSeleccionada = it
                    isLoading = false
                },
                onFailure = {
                    isLoading = false
                    errorMessage = "No se pudo cargar la ubicación solicitada"
                }
            )
        }
    }

}
