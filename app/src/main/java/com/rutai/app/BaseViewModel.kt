package com.rutai.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rutai.app.utils.BackendErrorMapper
import com.rutai.app.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * 🏗️ BaseViewModel - Solución estructural para llamadas de red seguras.
 * Garantiza que isLoading siempre regrese a false y centraliza la obtención del token.
 */
abstract class BaseViewModel(
    protected val context: Context,
    protected val sessionManager: SessionManager
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        protected set

    /**
     * Ejecuta una llamada de red de forma segura.
     * @param call Bloque que recibe el token actual y retorna un Result de la operación.
     * @param onSuccess Callback si la operación fue exitosa.
     * @param onError Callback opcional para manejar el error (ya mapeado por BackendErrorMapper).
     */
    protected fun <T> safeApiCall(
        call: suspend (token: String) -> Result<T>,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            isLoading = true
            try {
                val token = sessionManager.getAccessToken()
                    ?: throw Exception("AUTH_ERROR:SESSION_EXPIRED")

                call(token).fold(
                    onSuccess = { onSuccess(it) },
                    onFailure = { exception ->
                        val error = BackendErrorMapper.resolve(context, exception.message)
                        onError(error)
                    }
                )
            } catch (e: Exception) {
                val error = BackendErrorMapper.resolve(context, e.message)
                onError(error)
            } finally {
                // 🛑 SIEMPRE se apaga el spinner, pase lo que pase.
                isLoading = false
            }
        }
    }

    /**
     * Ejecuta una llamada de red que no requiere token de autenticación (ej: login, refresh).
     */
    protected fun <T> safeApiCallNoToken(
        call: suspend () -> Result<T>,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            isLoading = true
            try {
                call().fold(
                    onSuccess = { onSuccess(it) },
                    onFailure = { exception ->
                        val error = BackendErrorMapper.resolve(context, exception.message)
                        onError(error)
                    }
                )
            } catch (e: Exception) {
                val error = BackendErrorMapper.resolve(context, e.message)
                onError(error)
            } finally {
                isLoading = false
            }
        }
    }
}