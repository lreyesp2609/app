package com.example.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.models.GrupoCreate
import com.example.app.network.GrupoResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class GrupoState {
    object Idle : GrupoState()
    object Loading : GrupoState()
    data class Success(val grupo: GrupoResponse, val message: String) : GrupoState()
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
                    response.code() == 400 -> {
                        _grupoState.value = GrupoState.Error(
                            "Datos inválidos. Verifica el nombre del grupo."
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

    fun resetState() {
        _grupoState.value = GrupoState.Idle
    }
}