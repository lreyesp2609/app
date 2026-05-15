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
import com.rutai.app.utils.SessionManager
import kotlinx.coroutines.launch

class UbicacionesViewModel(
    private val context: Context,
    private val sessionManager: SessionManager
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

    private fun getToken(): String = sessionManager.getAccessToken() ?: ""

    fun crearUbicacion(
        ubicacion: UbicacionUsuarioCreate,
        callback: (success: Boolean, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            try {
                repository.crearUbicacion(getToken(), ubicacion).fold(
                    onSuccess = { nuevaUbicacion ->
                        ubicaciones = ubicaciones + nuevaUbicacion
                        callback(true, null)
                    },
                    onFailure = { exception ->
                        val error = BackendErrorMapper.resolve(context, exception.message)
                        callback(false, error)
                    }
                )
            } catch (e: Exception) {
                callback(false, BackendErrorMapper.resolve(context, e.message))
            } finally {
                isLoading = false
            }
        }
    }

    fun cargarUbicaciones() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                repository.obtenerUbicaciones(getToken()).fold(
                    onSuccess = {
                        ubicaciones = it
                    },
                    onFailure = {
                        errorMessage = context.getString(com.rutai.app.R.string.error_load_locations)
                    }
                )
            } catch (e: Exception) {
                errorMessage = context.getString(com.rutai.app.R.string.error_load_locations)
            } finally {
                isLoading = false
            }
        }
    }

    fun cargarUbicacionPorId(id: Int) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                repository.obtenerUbicacionPorId(getToken(), id).fold(
                    onSuccess = {
                        ubicacionSeleccionada = it
                    },
                    onFailure = {
                        errorMessage = context.getString(com.rutai.app.R.string.error_load_location_id)
                    }
                )
            } catch (e: Exception) {
                errorMessage = context.getString(com.rutai.app.R.string.error_load_location_id)
            } finally {
                isLoading = false
            }
        }
    }

    fun eliminarUbicacion(
        id: Int,
        notificationViewModel: NotificationViewModel
    ) {
        viewModelScope.launch {
            isLoading = true
            try {
                repository.eliminarUbicacion(getToken(), id).fold(
                    onSuccess = {
                        ubicaciones = ubicaciones.filter { it.id != id }
                        notificationViewModel.showSuccess(com.rutai.app.R.string.location_deleted_success_msg)
                    },
                    onFailure = { exception ->
                        val error = BackendErrorMapper.resolve(context, exception.message)
                        notificationViewModel.showError(error)
                    }
                )
            } catch (e: Exception) {
                notificationViewModel.showError(BackendErrorMapper.resolve(context, e.message))
            } finally {
                isLoading = false
            }
        }
    }
}
