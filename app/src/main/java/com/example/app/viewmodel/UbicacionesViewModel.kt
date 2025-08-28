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

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // 👇 Nueva variable para la ubicación individual
    var ubicacionSeleccionada by mutableStateOf<UbicacionUsuarioResponse?>(null)
        private set

    // --- métodos existentes ---
    fun crearUbicacion(ubicacion: UbicacionUsuarioCreate, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            repository.crearUbicacion(token, ubicacion).fold(onSuccess = {
                ubicaciones = ubicaciones + it
                isLoading = false
                onSuccess()
            }, onFailure = {
                errorMessage = it.message
                isLoading = false
            })
        }
    }

    fun cargarUbicaciones() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            repository.obtenerUbicaciones(token).fold(onSuccess = {
                ubicaciones = it
                isLoading = false
            }, onFailure = {
                errorMessage = it.message
                isLoading = false
            })
        }
    }

    fun cargarUbicacionPorId(id: Int) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            repository.obtenerUbicacionPorId(token, id).fold(onSuccess = {
                ubicacionSeleccionada = it
                isLoading = false
            }, onFailure = {
                errorMessage = it.message
                isLoading = false
            })
        }
    }
}
