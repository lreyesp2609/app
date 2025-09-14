package com.example.app.viewmodel

import android.content.Context
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.models.User
import com.example.app.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

class AuthViewModel(private val context: Context) : ViewModel() {
    private val repository = AuthRepository()
    private val sessionManager = SessionManager(context)

    // Estados de la UI
    var isLoading by mutableStateOf(false)
        private set
    var user by mutableStateOf<User?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isLoggedIn by mutableStateOf(false)
        private set
    var accessToken by mutableStateOf<String?>(null)
        private set

    init {
        // 🔹 Restaurar estado inmediatamente desde SharedPreferences
        restoreStateFromPrefs()
        restoreSession()
        startAutoRefresh()
    }

    // 🔹 Restaurar estado desde SharedPreferences
    private fun restoreStateFromPrefs() {
        isLoggedIn = sessionManager.isLoggedIn()
        accessToken = sessionManager.getAccessToken()
        user = sessionManager.getUser()
    }

    // 🔹 Restaurar sesión automáticamente al iniciar la app
    private fun restoreSession() {
        val savedRefresh = sessionManager.getRefreshToken()
        if (savedRefresh != null && sessionManager.hasValidSession()) {
            viewModelScope.launch {
                isLoading = true
                repository.refreshToken(savedRefresh).fold(
                    onSuccess = { response ->
                        accessToken = response.accessToken
                        isLoggedIn = true
                        sessionManager.saveTokens(response.accessToken, response.refreshToken)
                        sessionManager.saveLoginState(true)

                        // Solo obtener usuario si no lo tenemos guardado
                        if (user == null) {
                            getCurrentUser()
                        } else {
                            isLoading = false
                        }
                    },
                    onFailure = {
                        clearLocalSession() // Cambiar logout() por clearLocalSession()
                        isLoading = false
                    }
                )
            }
        } else if (!sessionManager.hasValidSession()) {
            // Si no hay sesión válida, limpiar estado pero sin loading
            clearLocalSession()
            isLoading = false // Asegurar que loading sea false
        } else {
            // Caso donde no hay refresh token
            isLoading = false
        }
    }

    // 🔹 Función de login (actualizada)
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null // Limpiar errores anteriores

            repository.login(email, password, Build.MODEL, getAppVersion(context), obtenerIp())
                .fold(
                    onSuccess = { loginResponse ->
                        accessToken = loginResponse.accessToken
                        isLoggedIn = true

                        sessionManager.saveTokens(loginResponse.accessToken, loginResponse.refreshToken)
                        sessionManager.saveLoginState(true)

                        getCurrentUser(onSuccess)
                    },
                    onFailure = { exception ->
                        isLoggedIn = false
                        sessionManager.saveLoginState(false)
                        isLoading = false

                        // Manejar errores de login de manera amigable
                        errorMessage = when {
                            exception.message?.contains("INVALID_CREDENTIALS") == true -> {
                                "Correo o contraseña incorrectos"
                            }
                            exception.message?.contains("USER_NOT_FOUND") == true -> {
                                "No existe una cuenta con este correo"
                            }
                            exception.message?.contains("ACCOUNT_LOCKED") == true -> {
                                "Cuenta bloqueada. Contacta soporte"
                            }
                            exception.message?.contains("NETWORK_ERROR") == true -> {
                                "Error de conexión. Verifica tu internet"
                            }
                            exception.message?.contains("401") == true -> {
                                "Credenciales inválidas"
                            }
                            exception.message?.contains("500") == true -> {
                                "Error del servidor. Inténtalo más tarde"
                            }
                            else -> {
                                "Error al iniciar sesión. Inténtalo nuevamente"
                            }
                        }
                    }
                )
        }
    }

    // 🔹 Función para obtener usuario (actualizada)
    fun getCurrentUser(onSuccess: () -> Unit = {}) {
        accessToken?.let { token ->
            viewModelScope.launch {
                isLoading = true
                repository.getCurrentUser("Bearer $token").fold(
                    onSuccess = { currentUser ->
                        user = currentUser
                        // 🔹 Guardar usuario en SharedPreferences
                        sessionManager.saveUser(currentUser)
                        isLoading = false
                        errorMessage = null
                        onSuccess()
                    },
                    onFailure = {
                        user = null
                        isLoggedIn = false
                        accessToken = null
                        sessionManager.saveLoginState(false)
                        isLoading = false
                        errorMessage = it.message
                    }
                )
            }
        } ?: run {
            user = null
            isLoggedIn = false
            sessionManager.saveLoginState(false)
            errorMessage = "No hay token de acceso"
            isLoading = false
        }
    }

    // 🔹 Logout (actualizado)
    fun logout(onComplete: (() -> Unit)? = null) {
        val savedRefresh = sessionManager.getRefreshToken()

        if (savedRefresh != null) {
            viewModelScope.launch {
                repository.logout(savedRefresh).fold(
                    onSuccess = {
                        clearLocalSession()
                        onComplete?.invoke()
                    },
                    onFailure = { exception ->
                        // Aunque falle el logout en backend, limpiamos localmente
                        clearLocalSession()
                        onComplete?.invoke()
                    }
                )
            }
        } else {
            // Si no hay refresh token, solo limpiar localmente
            clearLocalSession()
            onComplete?.invoke()
        }
    }

    // 🔹 Helper para limpiar sesión local
    private fun clearLocalSession() {
        user = null
        accessToken = null
        isLoggedIn = false
        isLoading = false
        errorMessage = null
        sessionManager.clear()
    }

    // 🔹 Auto refresh actualizado
    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000) // 5 minutos
                val savedRefresh = sessionManager.getRefreshToken()
                if (savedRefresh != null && isLoggedIn) {
                    repository.refreshToken(savedRefresh).fold(
                        onSuccess = { response ->
                            accessToken = response.accessToken
                            sessionManager.saveTokens(response.accessToken, response.refreshToken)
                        },
                        onFailure = {
                            logout()
                        }
                    )
                }
            }
        }
    }


    fun obtenerIp(): String {
        return try {
            val en = NetworkInterface.getNetworkInterfaces().toList()
            for (intf in en) {
                val addrs = intf.inetAddresses.toList()
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            "Desconocida"
        } catch (e: Exception) {
            "Desconocida"
        }
    }

    fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Desconocida"
        } catch (e: Exception) {
            "Desconocida"
        }
    }

    fun clearError() {
        errorMessage = null
    }

    class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    fun registerUser(nombre: String, apellido: String, email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null // Limpiar errores anteriores

            repository.register(nombre, apellido, email, password).fold(
                onSuccess = {
                    isLoading = false
                    onResult(true)
                },
                onFailure = { exception ->
                    isLoading = false

                    // Manejar diferentes tipos de errores del backend
                    errorMessage = when {
                        exception.message?.contains("USER_ALREADY_EXISTS") == true -> {
                            "Ya existe una cuenta con este correo electrónico"
                        }
                        exception.message?.contains("INVALID_EMAIL") == true -> {
                            "El formato del correo electrónico no es válido"
                        }
                        exception.message?.contains("WEAK_PASSWORD") == true -> {
                            "La contraseña debe ser más fuerte"
                        }
                        exception.message?.contains("NETWORK_ERROR") == true -> {
                            "Error de conexión. Verifica tu internet"
                        }
                        exception.message?.contains("SERVER_ERROR") == true -> {
                            "Error del servidor. Inténtalo más tarde"
                        }
                        exception.message?.contains("400") == true -> {
                            "Datos inválidos. Verifica la información ingresada"
                        }
                        exception.message?.contains("500") == true -> {
                            "Error interno del servidor. Inténtalo más tarde"
                        }
                        else -> {
                            // Mensaje genérico amigable
                            "Error al crear la cuenta. Inténtalo nuevamente"
                        }
                    }

                    onResult(false)
                }
            )
        }
    }
}