package com.rutai.app.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rutai.app.repository.AuthRepository
import com.rutai.app.utils.SessionManager

/**
 * 🛠️ TokenRefreshWorker - Refresco proactivo del JWT en segundo plano.
 * 
 * Este worker es gestionado por WorkManager y asegura que el token se mantenga
 * actualizado incluso si la app está en background o el proceso principal es suspendido.
 */
class TokenRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val sessionManager = SessionManager.getInstance(context)
    private val authRepository = AuthRepository()

    companion object {
        private const val TAG = "TokenRefreshWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🚀 Iniciando verificación de token programada")

        // 1️⃣ Verificar si el usuario ha iniciado sesión
        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "⏹️ Usuario no logueado. Finalizando worker.")
            return Result.success()
        }

        // 2️⃣ Verificar si el token realmente necesita refrescarse (Margen de 3 minutos)
        if (!sessionManager.isTokenExpiringSoon(marginMinutes = 3)) {
            Log.d(TAG, "✅ El token aún es válido por más de 3 minutos. Nada que hacer.")
            return Result.success()
        }

        // 3️⃣ Obtener el refresh token
        val refreshToken = sessionManager.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            Log.e(TAG, "❌ No se encontró un refresh token válido.")
            return Result.failure()
        }

        Log.d(TAG, "🔄 El token está por expirar. Intentando refresh proactivo...")

        return try {
            val result = authRepository.refreshToken(refreshToken)

            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response != null) {
                    // Guardamos los tokens y actualizamos la estampa de tiempo
                    sessionManager.saveTokens(
                        access = response.accessToken,
                        refresh = response.refreshToken
                    )
                    Log.d(TAG, "✅ Token refrescado exitosamente en background.")
                    Result.success()
                } else {
                    Log.w(TAG, "⚠️ Respuesta de refresh exitosa pero vacía.")
                    Result.retry()
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "UNKNOWN_ERROR"
                Log.w(TAG, "⚠️ Fallo en el refresh de background: $error")

                // Clasificamos el error para decidir si reintentar o abortar
                when {
                    error.contains("AUTH_ERROR") || error.contains("401") || error.contains("403") -> {
                        Log.e(TAG, "❌ Sesión inválida o expirada permanentemente. Abortando.")
                        // No limpiamos sesión aquí para evitar comportamientos inesperados en background,
                        // el interceptor lo hará cuando la app vuelva al primer plano si falla de nuevo.
                        Result.failure()
                    }
                    else -> {
                        // Error de red o servidor temporal, reintentar según la política de WorkManager
                        Log.d(TAG, "🔁 Reintentando worker debido a error temporal...")
                        Result.retry()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔥 Excepción crítica en el Worker: ${e.message}")
            Result.retry()
        }
    }
}
