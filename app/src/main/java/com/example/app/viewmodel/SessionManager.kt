package com.example.app.viewmodel

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)

    fun saveTokens(access: String, refresh: String) {
        prefs.edit()
            .putString("ACCESS_TOKEN", access)
            .putString("REFRESH_TOKEN", refresh)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString("ACCESS_TOKEN", null)
    fun getRefreshToken(): String? = prefs.getString("REFRESH_TOKEN", null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
