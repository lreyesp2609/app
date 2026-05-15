package com.rutai.app.utils

import android.content.Context
import android.util.Log
import com.rutai.app.models.User
import com.rutai.app.repository.AuthRepository
import com.google.gson.Gson
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

    fun saveTokens(access: String, refresh: String) {
        Log.d(TAG, "💾 ========================================")
        Log.d(TAG, "💾 GUARDANDO NUEVOS TOKENS")
        Log.d(TAG, "💾 ========================================")
        Log.d(TAG, "   Access Token: ${access.take(20)}...")
        Log.d(TAG, "   Refresh Token: ${refresh.take(20)}...")
        Log.d(TAG, "   Listeners registrados: ${tokenListeners.size}")

        prefs.edit()
            .putString("ACCESS_TOKEN", access)
            .putString("REFRESH_TOKEN", refresh)
            .apply()

        // 🆕 Notificar a todos los listeners
        if (tokenListeners.isEmpty()) {
            Log.w(TAG, "⚠️ ========================================")
            Log.w(TAG, "⚠️ NO HAY LISTENERS REGISTRADOS")
            Log.w(TAG, "⚠️ El token no será enviado al WebSocket")
            Log.w(TAG, "⚠️ ========================================")
        } else {
            Log.d(TAG, "📢 Notificando a ${tokenListeners.size} listeners...")
            tokenListeners.forEachIndexed { index, listener ->
                try {
                    Log.d(TAG, "   📤 Notificando listener #${index + 1}...")
                    listener.invoke(access)
                    Log.d(TAG, "   ✅ Listener #${index + 1} notificado correctamente")
                } catch (e: Exception) {
                    Log.e(TAG, "   ❌ Error al notificar listener #${index + 1}: ${e.message}")
                    e.printStackTrace()
                }
            }
            Log.d(TAG, "✅ ========================================")
            Log.d(TAG, "✅ TODOS LOS LISTENERS NOTIFICADOS")
            Log.d(TAG, "✅ ========================================")
        }
    }

    fun addTokenChangeListener(listener: (String) -> Unit) {
        tokenListeners.add(listener)
        Log.d(TAG, "➕ ========================================")
        Log.d(TAG, "➕ LISTENER REGISTRADO")
        Log.d(TAG, "➕ Total de listeners: ${tokenListeners.size}")
        Log.d(TAG, "➕ ========================================")
    }

    fun removeTokenChangeListener(listener: (String) -> Unit) {
        val removed = tokenListeners.remove(listener)
        Log.d(TAG, "➖ ========================================")
        Log.d(TAG, "➖ LISTENER ${if (removed) "REMOVIDO" else "NO ENCONTRADO"}")
        Log.d(TAG, "➖ Total de listeners: ${tokenListeners.size}")
        Log.d(TAG, "➖ ========================================")
    }

    // 🆕 Método para verificar cuántos listeners hay
    fun getListenerCount(): Int {
        return tokenListeners.size
    }

    fun getAccessToken(): String? {
        val token = prefs.getString("ACCESS_TOKEN", null)
        if (token != null) {
            Log.v(TAG, "🔑 Token recuperado: ${token.take(20)}...")
        } else {
            Log.w(TAG, "⚠️ No hay token disponible")
        }
        return token
    }

    fun getRefreshToken(): String? = prefs.getString("REFRESH_TOKEN", null)

    fun saveLoginState(isLoggedIn: Boolean) {
        prefs.edit()
            .putBoolean("IS_LOGGED_IN", isLoggedIn)
            .apply()
        Log.d(TAG, "🔐 Estado de login actualizado: $isLoggedIn")
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("IS_LOGGED_IN", false)

    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        prefs.edit()
            .putString("USER_DATA", userJson)
            .apply()
        Log.d(TAG, "👤 Usuario guardado: ${user.nombre} ${user.apellido}")
    }

    fun getUser(): User? {
        val userJson = prefs.getString("USER_DATA", null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al parsear usuario: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    fun clear() {
        Log.d(TAG, "🧹 Limpiando sesión (manteniendo listeners)")
        // ❌ NO borrar listeners aquí - deben persistir
        // tokenListeners.clear()
        prefs.edit().clear().apply()
        Log.d(TAG, "✅ Sesión limpiada. Listeners preservados: ${tokenListeners.size}")
    }

    fun hasValidSession(): Boolean {
        return getRefreshToken() != null && isLoggedIn()
    }

    fun startAutoRefreshIfNeeded(onAuthExpired: () -> Unit = {}) {
        if (autoRefreshJob?.isActive == true) {
            Log.d(TAG, "🔁 Auto-refresh ya está activo (singleton)")
            return
        }

        autoRefreshJob = refreshScope.launch {
            Log.d(TAG, "🚀 Auto-refresh singleton iniciado")
            var consecutiveFailures = 0

            while (isActive) {
                delay(REFRESH_INTERVAL_MS)

                val refreshToken = getRefreshToken()
                val loggedIn = isLoggedIn()
                if (refreshToken.isNullOrEmpty() || !loggedIn) {
                    continue
                }

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

                        // ⚠️ Solo cerrar sesión por errores de autenticación reales.
                        // Errores de red/intermitencia NO deben limpiar credenciales.
                        if (isAuthError) {
                            saveLoginState(false)
                            clear()
                            onAuthExpired()
                            consecutiveFailures = 0
                        } else if (consecutiveFailures >= MAX_REFRESH_FAILURES) {
                            Log.w(TAG, "🌐 Fallos de red consecutivos en auto-refresh. Se mantiene sesión local.")
                            consecutiveFailures = 0
                        }
                    }
                )
            }
        }
    }

}
