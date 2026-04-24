package com.example.app.viewmodel

import android.content.Context
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

class GrupoViewModel(
    private val context: Context,
    private val repository: GrupoRepository
) : ViewModel() {

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
                            message = context.getString(com.example.app.R.string.group_created_success)
                        )
                    }
                    response.code() == 400 -> {
                        _grupoState.value = GrupoState.Error(
                            context.getString(com.example.app.R.string.error_group_name_exists)
                        )
                    }
                    response.code() == 401 -> {
                        _grupoState.value = GrupoState.Error(
                            context.getString(com.example.app.R.string.error_session_expired)
                        )
                    }
                    response.code() == 500 -> {
                        _grupoState.value = GrupoState.Error(
                            context.getString(com.example.app.R.string.error_server_internal)
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
                        is java.net.UnknownHostException -> context.getString(com.example.app.R.string.error_no_internet)
                        is java.net.SocketTimeoutException -> context.getString(com.example.app.R.string.error_timeout)
                        else -> e.localizedMessage ?: context.getString(com.example.app.R.string.error_unknown_create_group)
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
                    _grupoState.value = GrupoState.Error(
                        context.getString(com.example.app.R.string.error_getting_groups, response.message())
                    )
                }
            } catch (e: Exception) {
                _grupoState.value = GrupoState.Error(
                    context.getString(com.example.app.R.string.error_generic_message, e.localizedMessage)
                )
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
                            message = context.getString(com.example.app.R.string.join_group_success)
                        )
                    }
                    response.code() == 404 -> {
                        _grupoState.value = GrupoState.Error(
                            context.getString(com.example.app.R.string.error_invalid_code)
                        )
                    }
                    response.code() == 400 -> {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = when {
                            errorBody?.contains("Ya perteneces") == true ->
                                context.getString(com.example.app.R.string.error_already_in_group)
                            errorBody?.contains("creador") == true ->
                                context.getString(com.example.app.R.string.error_creator_already_in_group)
                            else ->
                                errorBody ?: context.getString(com.example.app.R.string.error_join_group)
                        }
                        _grupoState.value = GrupoState.Error(errorMessage)
                    }
                    response.code() == 401 -> {
                        _grupoState.value = GrupoState.Error(
                            context.getString(com.example.app.R.string.error_session_expired_join)
                        )
                        shouldReloadList = false
                    }
                    response.code() == 500 -> {
                        _grupoState.value = GrupoState.Error(
                            context.getString(com.example.app.R.string.error_server_internal_short)
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
                        is java.net.UnknownHostException -> context.getString(com.example.app.R.string.error_no_internet_join)
                        is java.net.SocketTimeoutException -> context.getString(com.example.app.R.string.error_timeout_join)
                        else -> e.localizedMessage ?: context.getString(com.example.app.R.string.error_unknown_join)
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
                    _mensajeSalida.value = response.body()?.message ?: context.getString(com.example.app.R.string.exit_group_success_msg)
                } else {
                    _mensajeSalida.value = context.getString(com.example.app.R.string.error_exit_group, response.errorBody()?.string())
                }
            } catch (e: Exception) {
                _mensajeSalida.value = context.getString(com.example.app.R.string.error_connection_exit_group, e.localizedMessage)
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
                    _mensajeEliminacion.value = response.body()?.message ?: context.getString(com.example.app.R.string.delete_group_success_msg)
                } else {
                    _mensajeEliminacion.value = context.getString(com.example.app.R.string.error_delete_group, response.errorBody()?.string())
                }
            } catch (e: Exception) {
                _mensajeEliminacion.value = context.getString(com.example.app.R.string.error_connection_delete_group, e.localizedMessage)
            }
        }
    }

    fun resetMensajeEliminacion() {
        _mensajeEliminacion.value = null
    }
}