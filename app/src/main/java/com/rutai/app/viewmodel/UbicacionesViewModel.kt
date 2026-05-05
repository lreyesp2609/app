package com.rutai.app.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutai.app.models.UbicacionUsuarioCreate
import com.rutai.app.models.UbicacionUsuarioResponse
import com.rutai.app.repository.UbicacionesRepository
import com.rutai.app.utils.BackendErrorMapper
import kotlinx.coroutines.launch

class UbicacionesViewModel(
    private val context: Context,
    private val token: String
) : ViewModel() {

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
                    val error = BackendErrorMapper.resolve(context, exception.message)
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
                    errorMessage = context.getString(com.rutai.app.R.string.error_load_locations)
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
                    errorMessage = context.getString(com.rutai.app.R.string.error_load_location_id)
                }
            )
        }
    }
    fun eliminarUbicacion(
        id: Int,
        notificationViewModel: NotificationViewModel
    ) {
        viewModelScope.launch {
            isLoading = true

            repository.eliminarUbicacion(token, id).fold(
                onSuccess = {
                    // Quitar de la lista
                    ubicaciones = ubicaciones.filter { it.id != id }

                    isLoading = false
                    notificationViewModel.showSuccess(com.rutai.app.R.string.location_deleted_success_msg)
                },
                onFailure = { exception ->
                    isLoading = false
                    val error = BackendErrorMapper.resolve(context, exception.message)
                    notificationViewModel.showError(error)
                }
            )
        }
    }

}
