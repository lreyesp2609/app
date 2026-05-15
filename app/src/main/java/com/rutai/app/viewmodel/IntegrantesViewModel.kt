package com.rutai.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rutai.app.BaseViewModel
import com.rutai.app.models.IntegranteGrupo
import com.rutai.app.network.RetrofitClient
import com.rutai.app.repository.GrupoRepository
import com.rutai.app.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IntegrantesViewModel(
    private val repository: GrupoRepository,
    sessionManager: SessionManager,
    context: Context
) : BaseViewModel(context, sessionManager) {

    private val _integrantes = MutableStateFlow<List<IntegranteGrupo>>(emptyList())
    val integrantes: StateFlow<List<IntegranteGrupo>> = _integrantes.asStateFlow()

    private val _grupoNombre = MutableStateFlow("")
    val grupoNombre: StateFlow<String> = _grupoNombre.asStateFlow()

    private val _totalIntegrantes = MutableStateFlow(0)
    val totalIntegrantes: StateFlow<Int> = _totalIntegrantes.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun cargarIntegrantes(grupoId: Int) {
        _error.value = null
        safeApiCall(
            call = { token -> repository.obtenerIntegrantes(token, grupoId) },
            onSuccess = { data ->
                _integrantes.value = data.integrantes
                _grupoNombre.value = data.grupo_nombre
                _totalIntegrantes.value = data.total_integrantes
            },
            onError = { errorMsg ->
                _error.value = errorMsg
                Log.e("IntegrantesVM", "❌ Error: $errorMsg")
            }
        )
    }

    fun limpiarError() {
        _error.value = null
    }
}

class IntegrantesViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IntegrantesViewModel::class.java)) {
            val repository = GrupoRepository(RetrofitClient.grupoService)
            val sessionManager = SessionManager.getInstance(context)
            @Suppress("UNCHECKED_CAST")
            return IntegrantesViewModel(repository, sessionManager, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
