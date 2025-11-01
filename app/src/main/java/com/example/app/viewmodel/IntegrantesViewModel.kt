package com.example.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.models.IntegranteGrupo
import com.example.app.network.RetrofitClient
import com.example.app.repository.GrupoRepository
import com.example.app.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IntegrantesViewModel(
    private val repository: GrupoRepository,
    private val context: Context
) : ViewModel() {

    private val _integrantes = MutableStateFlow<List<IntegranteGrupo>>(emptyList())
    val integrantes: StateFlow<List<IntegranteGrupo>> = _integrantes.asStateFlow()

    private val _grupoNombre = MutableStateFlow("")
    val grupoNombre: StateFlow<String> = _grupoNombre.asStateFlow()

    private val _totalIntegrantes = MutableStateFlow(0)
    val totalIntegrantes: StateFlow<Int> = _totalIntegrantes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun cargarIntegrantes(grupoId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val sessionManager = SessionManager.getInstance(context)
                val token = sessionManager.getAccessToken()

                Log.d("IntegrantesVM", "🔑 Token obtenido: ${token?.take(20)}...")

                if (token.isNullOrEmpty()) {
                    _error.value = "No hay sesión activa"
                    Log.e("IntegrantesVM", "❌ No se encontró token de acceso")
                    _isLoading.value = false
                    return@launch
                }

                Log.d("IntegrantesVM", "📡 Haciendo petición a /grupos/$grupoId/integrantes")

                val response = repository.obtenerIntegrantes(token, grupoId)

                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        _integrantes.value = data.integrantes
                        _grupoNombre.value = data.grupo_nombre
                        _totalIntegrantes.value = data.total_integrantes
                        Log.d("IntegrantesVM", "✅ Cargados ${data.total_integrantes} integrantes")
                    } ?: run {
                        _error.value = "No se recibieron datos"
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _error.value = when (response.code()) {
                        403 -> "No tienes permiso para ver los integrantes"
                        404 -> "Grupo no encontrado"
                        else -> "Error al cargar integrantes: ${response.code()}"
                    }
                    Log.e("IntegrantesVM", "❌ Error: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
                Log.e("IntegrantesVM", "❌ Excepción: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun limpiarError() {
        _error.value = null
    }
}

// Factory para crear el ViewModel
class IntegrantesViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IntegrantesViewModel::class.java)) {
            val repository = GrupoRepository(RetrofitClient.grupoService)
            @Suppress("UNCHECKED_CAST")
            return IntegrantesViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}