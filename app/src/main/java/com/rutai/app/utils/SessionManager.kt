package com.rutai.app.utils

import android.content.Context
import android.util.Log
import com.rutai.app.repository.AuthRepository
import com.google.gson.Gson
import com.rutai.app.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SessionManager private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val authRepository by lazy { AuthRepository() }
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var autoRefreshJob: Job? = null

    // 🆕 Listener para cambios de token
    private val tokenListeners = mutableListOf<(String) -> Unit>()

    companion object {
        private const val TAG = "WS_SessionManager"
        private const val REFRESH_INTERVAL_MS = 4 * 60 * 1000L
        private const val MAX_REFRESH_FAILURES = 3
        
        // Claves de SharedPreferences
        private const val ACCESS_TOKEN = "ACCESS_TOKEN"
        private const val REFRESH_TOKEN = "REFRESH_TOKEN"
        private const val TOKEN_EXPIRES_AT = "TOKEN_EXPIRES_AT"
        private const val IS_LOGGED_IN = "IS_LOGGED_IN"
        private const val USER_DATA = "USER_DATA"
        private const val FCM_TOKEN = "FCM_TOKEN"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also {
                    INSTANCE = it
                    Log.d(TAG, "🏗️ SessionManager Singleton creado")
                }
            }
        }
    }

    // Dentro de SessionManager.kt, añade estas constantes y métodos
    fun saveFcmToken(token: String) {
        prefs.edit().putString(FCM_TOKEN, token).apply()
        Log.d(TAG, "📲 Token FCM guardado localmente")
    }

    fun getFcmToken(): String? = prefs.getString(FCM_TOKEN, null)


    /**
     * Guarda los tokens y calcula el tiempo de expiración.
     * @param expiresInSeconds Tiempo de vida del token en segundos (default 15 min / 900s)
     */
    fun saveTokens(access: String, refresh: String, expiresInSeconds: Long = 900) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000)
        
        Log.d(TAG, "💾 ========================================")
        Log.d(TAG, "💾 GUARDANDO NUEVOS TOKENS")
        Log.d(TAG, "💾 Expiración calculada: $expiresAt ms")
        Log.d(TAG, "💾 ========================================")

        prefs.edit()
            .putString(ACCESS_TOKEN, access)
            .putString(REFRESH_TOKEN, refresh)
            .putLong(TOKEN_EXPIRES_AT, expiresAt)
            .apply()

        // Notificar a todos los listeners
        if (tokenListeners.isEmpty()) {
            Log.w(TAG, "⚠️ NO HAY LISTENERS REGISTRADOS")
        } else {
            Log.d(TAG, "📢 Notificando a ${tokenListeners.size} listeners...")
            tokenListeners.forEach { listener ->
                try {
                    listener.invoke(access)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al notificar listener: ${e.message}")
                }
            }
        }
    }

    /**
     * Verifica si el token expirará pronto según un margen de minutos.
     */
    fun isTokenExpiringSoon(marginMinutes: Int = 3): Boolean {
        val expiresAt = prefs.getLong(TOKEN_EXPIRES_AT, 0)
        if (expiresAt == 0L) return true
        
        val currentTime = System.currentTimeMillis()
        val marginMs = marginMinutes * 60 * 1000L
        return (expiresAt - currentTime) < marginMs
    }

    /**
     * Verifica si el token ya expiró.
     */
    fun isTokenExpired(): Boolean {
        val expiresAt = prefs.getLong(TOKEN_EXPIRES_AT, 0)
        return System.currentTimeMillis() >= expiresAt
    }

    fun addTokenChangeListener(listener: (String) -> Unit) {
        tokenListeners.add(listener)
        Log.d(TAG, "➕ LISTENER REGISTRADO. Total: ${tokenListeners.size}")
    }

    fun removeTokenChangeListener(listener: (String) -> Unit) {
        tokenListeners.remove(listener)
        Log.d(TAG, "➖ LISTENER REMOVIDO. Total: ${tokenListeners.size}")
    }

    fun getListenerCount(): Int = tokenListeners.size

    fun getAccessToken(): String? = prefs.getString(ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(REFRESH_TOKEN, null)

    fun saveLoginState(isLoggedIn: Boolean) {
        prefs.edit()
            .putBoolean(IS_LOGGED_IN, isLoggedIn)
            .apply()
        Log.d(TAG, "🔐 Estado de login actualizado: $isLoggedIn")
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(IS_LOGGED_IN, false)

    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        prefs.edit()
            .putString(USER_DATA, userJson)
            .apply()
    }

    fun getUser(): User? {
        val userJson = prefs.getString(USER_DATA, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    fun clear() {
        Log.d(TAG, "🧹 Limpiando sesión")
        prefs.edit().clear().apply()
    }

    fun hasValidSession(): Boolean {
        return getRefreshToken() != null && isLoggedIn()
    }

    /**
     * Detiene el loop de auto-refresco.
     */
    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
        Log.d(TAG, "🛑 Auto-refresh detenido")
    }

    fun startAutoRefreshIfNeeded(onAuthExpired: () -> Unit = {}) {
        if (autoRefreshJob?.isActive == true) {
            Log.d(TAG, "🔁 Auto-refresh ya está activo")
            return
        }

        autoRefreshJob = refreshScope.launch {
            Log.d(TAG, "🚀 Auto-refresh iniciado")
            var consecutiveFailures = 0

            while (isActive) {
                delay(REFRESH_INTERVAL_MS)

                val refreshToken = getRefreshToken()
                val loggedIn = isLoggedIn()
                
                if (refreshToken.isNullOrEmpty() || !loggedIn) continue

                // Solo refrescar si está por expirar o ha pasado el intervalo
                authRepository.refreshToken(refreshToken).fold(
                    onSuccess = { response ->
                        consecutiveFailures = 0
                        saveTokens(response.accessToken, response.refreshToken)
                        Log.d(TAG, "✅ Auto-refresh exitoso")
                    },
                    onFailure = { error ->
                        consecutiveFailures++
                        val isAuthError = error.message?.contains("401") == true ||
                                error.message?.contains("AUTH_ERROR") == true

                        Log.w(TAG, "⚠️ Auto-refresh falló (#$consecutiveFailures): ${error.message}")

                        if (isAuthError) {
                            saveLoginState(false)
                            clear()
                            onAuthExpired()
                            consecutiveFailures = 0
                            stopAutoRefresh()
                        } else if (consecutiveFailures >= MAX_REFRESH_FAILURES) {
                            Log.w(TAG, "🌐 Fallos de red consecutivos. Se mantiene sesión local.")
                            consecutiveFailures = 0
                        }
                    }
                )
            }
        }
    }
}
