package com.example.app.utils

import android.content.Context
import android.util.Log
import com.example.app.models.User
import com.google.gson.Gson

class SessionManager private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // 🆕 Listener para cambios de token
    private val tokenListeners = mutableListOf<(String) -> Unit>()

    companion object {
        private const val TAG = "WS_SessionManager"

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
        Log.d(TAG, "🧹 Limpiando sesión y listeners")
        tokenListeners.clear()
        prefs.edit().clear().apply()
    }

    fun hasValidSession(): Boolean {
        return getRefreshToken() != null && isLoggedIn()
    }
}