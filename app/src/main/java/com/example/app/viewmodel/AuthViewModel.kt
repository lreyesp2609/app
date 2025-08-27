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
        restoreSession()
        startAutoRefresh()
    }


    // 🔹 Restaurar sesión automáticamente al iniciar la app
    private fun restoreSession() {
        val savedRefresh = sessionManager.getRefreshToken()
        if (savedRefresh != null) {
            viewModelScope.launch {
                isLoading = true
                repository.refreshToken(savedRefresh).fold(
                    onSuccess = { response ->
                        accessToken = response.accessToken
                        isLoggedIn = true
                        sessionManager.saveTokens(response.accessToken, response.refreshToken)
                        getCurrentUser()
                    },
                    onFailure = {
                        logout()
                        isLoading = false
                    }
                )
            }
        }
    }

    // 🔹 Función de login
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            repository.login(email, password, Build.MODEL, getAppVersion(context), obtenerIp())
                .fold(
                    onSuccess = { loginResponse ->
                        accessToken = loginResponse.accessToken
                        isLoggedIn = true

                        // Guardar tokens localmente
                        sessionManager.saveTokens(loginResponse.accessToken, loginResponse.refreshToken)

                        getCurrentUser(onSuccess)
                    },
                    onFailure = { exception ->
                        errorMessage = exception.message ?: "Error desconocido"
                        isLoggedIn = false
                        isLoading = false
                    }
                )
        }
    }

    // 🔹 Función para obtener usuario
    fun getCurrentUser(onSuccess: () -> Unit = {}) {
        accessToken?.let { token ->
            viewModelScope.launch {
                isLoading = true
                repository.getCurrentUser("Bearer $token").fold(
                    onSuccess = { currentUser ->
                        user = currentUser
                        isLoading = false
                        errorMessage = null
                        onSuccess()
                    },
                    onFailure = {
                        user = null
                        isLoggedIn = false
                        accessToken = null
                        isLoading = false
                        errorMessage = it.message
                    }
                )
            }
        } ?: run {
            user = null
            isLoggedIn = false
            errorMessage = "No hay token de acceso"
            isLoading = false
        }
    }


    // 🔹 Logout
    fun logout() {
        user = null
        accessToken = null
        isLoggedIn = false
        isLoading = false
        errorMessage = null
        sessionManager.clear()
    }

    // 🔹 Reintento
    fun retryGetCurrentUser() {
        errorMessage = null
        getCurrentUser()
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

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000)
                val savedRefresh = sessionManager.getRefreshToken()
                if (savedRefresh != null) {
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
}