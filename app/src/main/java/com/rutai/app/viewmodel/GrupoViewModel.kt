package com.rutai.app.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.rutai.app.BaseViewModel
import com.rutai.app.models.GrupoCreate
import com.rutai.app.models.GrupoResponse
import com.rutai.app.repository.GrupoRepository
import com.rutai.app.utils.SessionManager
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

class GrupoViewModel(
    context: Context,
    private val repository: GrupoRepository,
    sessionManager: SessionManager
) : BaseViewModel(context, sessionManager) {

    private val _grupoState = MutableStateFlow<GrupoState>(GrupoState.Idle)
    val grupoState: StateFlow<GrupoState> = _grupoState

    fun createGrupo(grupoCreate: GrupoCreate) {
        _grupoState.value = GrupoState.Loading
        safeApiCall(
            call = { token -> repository.createGrupo(token, grupoCreate) },
            onSuccess = { grupo ->
                _grupoState.value = GrupoState.Success(
                    grupo = grupo,
                    message = context.getString(com.rutai.app.R.string.group_created_success)
                )
            },
            onError = { error ->
                _grupoState.value = GrupoState.Error(error)
            }
        )
    }

    fun listarGrupos(showLoading: Boolean = true) {
        if (showLoading) _grupoState.value = GrupoState.Loading
        safeApiCall(
            call = { token -> repository.listarGrupos(token) },
            onSuccess = { grupos ->
                _grupoState.value = GrupoState.ListSuccess(grupos)
            },
            onError = { error ->
                _grupoState.value = GrupoState.Error(error)
            }
        )
    }

    fun unirseAGrupo(codigo: String) {
        _grupoState.value = GrupoState.Loading
        safeApiCall(
            call = { token -> repository.unirseAGrupo(token, codigo) },
            onSuccess = { grupo ->
                _grupoState.value = GrupoState.JoinSuccess(
                    grupo = grupo,
                    message = context.getString(com.rutai.app.R.string.join_group_success)
                )
                // Recargar lista tras unirse
                viewModelScope.launch {
                    delay(500)
                    listarGrupos(showLoading = false)
                }
            },
            onError = { error ->
                _grupoState.value = GrupoState.Error(error)
                // Recargar lista incluso en error si no es de autenticación
                if (!error.contains("Sesión expirada", ignoreCase = true)) {
                    viewModelScope.launch {
                        delay(500)
                        listarGrupos(showLoading = false)
                    }
                }
            }
        )
    }

    fun resetState() { _grupoState.value = GrupoState.Idle }

    private val _mensajeSalida = MutableStateFlow<String?>(null)
    val mensajeSalida: StateFlow<String?> get() = _mensajeSalida

    fun salirDelGrupo(grupoId: Int) {
        safeApiCall(
            call = { token -> repository.salirDelGrupo(token, grupoId) },
            onSuccess = { response ->
                _mensajeSalida.value = response.message ?: context.getString(com.rutai.app.R.string.exit_group_success_msg)
                listarGrupos(showLoading = false)
            },
            onError = { error ->
                _mensajeSalida.value = error
            }
        )
    }

    fun resetMensajeSalida() { _mensajeSalida.value = null }

    private val _mensajeEliminacion = MutableStateFlow<String?>(null)
    val mensajeEliminacion: StateFlow<String?> get() = _mensajeEliminacion

    fun eliminarGrupo(grupoId: Int) {
        safeApiCall(
            call = { token -> repository.eliminarGrupo(token, grupoId) },
            onSuccess = { response ->
                _mensajeEliminacion.value = response.message ?: context.getString(com.rutai.app.R.string.delete_group_success_msg)
                listarGrupos(showLoading = false)
            },
            onError = { error ->
                _mensajeEliminacion.value = error
            }
        )
    }

    fun resetMensajeEliminacion() { _mensajeEliminacion.value = null }
}
