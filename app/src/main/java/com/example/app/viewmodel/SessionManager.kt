package com.example.app.viewmodel

import android.content.Context
import com.google.gson.Gson
import com.example.app.models.User

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveTokens(access: String, refresh: String) {
        prefs.edit()
            .putString("ACCESS_TOKEN", access)
            .putString("REFRESH_TOKEN", refresh)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString("ACCESS_TOKEN", null)
    fun getRefreshToken(): String? = prefs.getString("REFRESH_TOKEN", null)

    // 🔹 Guardar estado de login
    fun saveLoginState(isLoggedIn: Boolean) {
        prefs.edit()
            .putBoolean("IS_LOGGED_IN", isLoggedIn)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("IS_LOGGED_IN", false)

    // 🔹 Guardar información del usuario
    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        prefs.edit()
            .putString("USER_DATA", userJson)
            .apply()
    }

    fun getUser(): User? {
        val userJson = prefs.getString("USER_DATA", null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasValidSession(): Boolean {
        return getRefreshToken() != null && isLoggedIn()
    }
}