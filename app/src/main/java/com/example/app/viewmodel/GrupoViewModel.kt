package com.example.app.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.models.GrupoCreate
import com.example.app.models.GrupoResponse
import com.example.app.repository.GrupoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class GrupoState {
    object Idle : GrupoState()
    object Loading : GrupoState()
    data class Success(val grupo: GrupoResponse, val message: String) : GrupoState()
    data class ListSuccess(val grupos: List<GrupoResponse>) : GrupoState()
    data class JoinSuccess(val grupo: GrupoResponse, val message: String) : GrupoState()
    data class Error(val message: String) : GrupoState()
}

class GrupoViewModel(private val repository: GrupoRepository) : ViewModel() {

    private val _grupoState = MutableStateFlow<GrupoState>(GrupoState.Idle)
    val grupoState: StateFlow<GrupoState> = _grupoState

    fun createGrupo(token: String, grupoCreate: GrupoCreate) {
        viewModelScope.launch {
            _grupoState.value = GrupoState.Loading
            try {
                val response = repository.createGrupo(token, grupoCreate)

                when {
                    response.isSuccessful && response.body() != null -> {
                        _grupoState.value = GrupoState.Success(
                            grupo = response.body()!!,
                            message = "¡Grupo creado exitosamente!"
                        )
                    }
                    response.code() == 400 -> {
                        _grupoState.value = GrupoState.Error(
                            "Ya existe un grupo con ese nombre. Intenta con otro nombre."
                        )
                    }
                    response.code() == 401 -> {
                        _grupoState.value = GrupoState.Error(
                            "Tu sesión ha expirado. Por favor, inicia sesión nuevamente."
                        )
                    }
                    response.code() == 500 -> {
                        _grupoState.value = GrupoState.Error(
                            "Error en el servidor. Intenta de nuevo más tarde."
                        )
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        _grupoState.value = GrupoState.Error(
                            errorBody ?: "Error desconocido: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _grupoState.value = GrupoState.Error(
                    when (e) {
                        is java.net.UnknownHostException -> "Sin conexión a internet. Verifica tu conexión."
                        is java.net.SocketTimeoutException -> "La solicitud tardó demasiado. Intenta de nuevo."
                        else -> e.localizedMessage ?: "Error desconocido al crear el grupo"
                    }
                )
            }
        }
    }

    // ✅ CORREGIDO - Ahora usa el endpoint correcto
    fun listarGrupos(token: String, showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _grupoState.value = GrupoState.Loading
            }

            try {
                // ✅ Usar el endpoint que SÍ existe en el servidor
                val response = repository.listarGrupos(token)

                if (response.isSuccessful && response.body() != null) {
                    _grupoState.value = GrupoState.ListSuccess(response.body()!!)
                } else {
                    _grupoState.value = GrupoState.Error("Error al obtener grupos: ${response.message()}")
                }
            } catch (e: Exception) {
                _grupoState.value = GrupoState.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    fun unirseAGrupo(token: String, codigo: String) {
        viewModelScope.launch {
            _grupoState.value = GrupoState.Loading

            var shouldReloadList = true

            try {
                val response = repository.unirseAGrupo(token, codigo)

                when {
                    response.isSuccessful && response.body() != null -> {
                        _grupoState.value = GrupoState.JoinSuccess(
                            grupo = response.body()!!,
                            message = "¡Te uniste al grupo exitosamente!"
                        )
                    }
                    response.code() == 404 -> {
                        _grupoState.value = GrupoState.Error(
                            "El código de invitación no es válido o el grupo no existe."
                        )
                    }
                    response.code() == 400 -> {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = when {
                            errorBody?.contains("Ya perteneces") == true ->
                                "Ya perteneces a este grupo."
                            errorBody?.contains("creador") == true ->
                                "Eres el creador de este grupo, ya perteneces a él."
                            else ->
                                errorBody ?: "Error al unirse al grupo."
                        }
                        _grupoState.value = GrupoState.Error(errorMessage)
                    }
                    response.code() == 401 -> {
                        _grupoState.value = GrupoState.Error(
                            "Tu sesión ha expirado. Inicia sesión nuevamente."
                        )
                        shouldReloadList = false
                    }
                    response.code() == 500 -> {
                        _grupoState.value = GrupoState.Error(
                            "Error del servidor. Intenta más tarde."
                        )
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        _grupoState.value = GrupoState.Error(
                            errorBody ?: "Error desconocido: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _grupoState.value = GrupoState.Error(
                    when (e) {
                        is java.net.UnknownHostException -> "Sin conexión a internet. Verifica tu red."
                        is java.net.SocketTimeoutException -> "La solicitud tardó demasiado. Intenta de nuevo."
                        else -> e.localizedMessage ?: "Error desconocido al unirse al grupo."
                    }
                )
            }

            // ✅ Recargar la lista SIN mostrar loading
            if (shouldReloadList) {
                delay(500)
                listarGrupos(token, showLoading = false)
            }
        }
    }

    fun resetState() {
        _grupoState.value = GrupoState.Idle
    }

    private val _mensajeSalida = MutableStateFlow<String?>(null)
    val mensajeSalida: StateFlow<String?> get() = _mensajeSalida

    fun salirDelGrupo(token: String, grupoId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.salirDelGrupo(token, grupoId)
                if (response.isSuccessful) {
                    _mensajeSalida.value = response.body()?.message ?: "Has salido del grupo"
                } else {
                    _mensajeSalida.value = "Error: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _mensajeSalida.value = "Error de conexión: ${e.localizedMessage}"
            }
        }
    }

    fun resetMensajeSalida() {
        _mensajeSalida.value = null
    }

    private val _mensajeEliminacion = MutableStateFlow<String?>(null)
    val mensajeEliminacion: StateFlow<String?> get() = _mensajeEliminacion

    fun eliminarGrupo(token: String, grupoId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.eliminarGrupo(token, grupoId)
                if (response.isSuccessful) {
                    _mensajeEliminacion.value = response.body()?.message ?: "Grupo eliminado exitosamente"
                } else {
                    _mensajeEliminacion.value = "Error al eliminar grupo: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _mensajeEliminacion.value = "Error de conexión: ${e.localizedMessage}"
            }
        }
    }

    fun resetMensajeEliminacion() {
        _mensajeEliminacion.value = null
    }
}