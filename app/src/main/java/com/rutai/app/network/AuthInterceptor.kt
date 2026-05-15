package com.rutai.app.network

import android.content.Context
import android.content.Intent
import android.util.Log
import com.rutai.app.repository.AuthRepository
import com.rutai.app.utils.SessionManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 🔐 AuthInterceptor - Refresca el token automáticamente cuando expira
 *
 * Flujo:
 * 1. Detecta respuesta 401 (Token expirado)
 * 2. Intenta refrescar el token usando refresh_token (Sincronizado con Mutex)
 * 3. Si el refresh es exitoso, reintenta el request original
 * 4. Si el refresh falla, fuerza logout
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    private val sessionManager by lazy { SessionManager.getInstance(context) }
    private val authRepository by lazy { AuthRepository() }

    companion object {
        private const val TAG = "AuthInterceptor"
        private val mutex = Mutex()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1️⃣ Ejecutar request original
        var response = chain.proceed(originalRequest)

        // 2️⃣ Si es 401 Y NO es el endpoint de refresh/login, intentar refrescar token
        if (response.code == 401 && !isAuthEndpoint(originalRequest.url.toString())) {
            Log.w(TAG, "⚠️ 401 DETECTADO - Token expirado en: ${originalRequest.url}")

            // Intentar refresh sincronizado con Mutex
            val refreshResult = runBlocking {
                mutex.withLock {
                    val tokenActual = sessionManager.getAccessToken()
                    val tokenEnRequest = originalRequest.header("Authorization")?.removePrefix("Bearer ")

                    // Si el token ya cambió en SessionManager, alguien más ya hizo el refresh
                    if (tokenActual != null && tokenActual != tokenEnRequest) {
                        Log.d(TAG, "🔄 Token ya refrescado por otra petición")
                        Result.success(tokenActual)
                    } else {
                        intentarRefreshToken()
                    }
                }
            }

            if (refreshResult.isSuccess) {
                val nuevoAccessToken = refreshResult.getOrNull()
                if (nuevoAccessToken != null) {
                    response.close() // Cerrar la respuesta 401 anterior

                    val newRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $nuevoAccessToken")
                        .build()

                    response = chain.proceed(newRequest)
                }
            } else {
                val error = refreshResult.exceptionOrNull()?.message
                Log.e(TAG, "❌ Falló el refresh: $error. Forzando logout...")

                forzarLogout(error)
            }
        }

        return response
    }

    private suspend fun intentarRefreshToken(): Result<String?> {
        return try {
            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                return Result.failure(Exception("AUTH_ERROR:NO_REFRESH_TOKEN"))
            }

            val result = authRepository.refreshToken(refreshToken)
            if (result.isSuccess) {
                val loginResponse = result.getOrNull()
                if (loginResponse != null) {
                    sessionManager.saveTokens(
                        access = loginResponse.accessToken,
                        refresh = loginResponse.refreshToken
                    )
                    Result.success(loginResponse.accessToken)
                } else {
                    Result.failure(Exception("AUTH_ERROR:REFRESH_RESPONSE_NULL"))
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "UNKNOWN_ERROR"
                // Clasificar si es error fatal de sesión
                if (error.contains("AUTH_ERROR") || error.contains("401") || error.contains("403")) {
                    Result.failure(Exception("FORCE_LOGOUT:$error"))
                } else {
                    Result.failure(Exception(error))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isAuthEndpoint(url: String): Boolean {
        val normalizedUrl = url.substringBefore("?").trimEnd('/')

        // Solo excluimos endpoints de autenticación que no deben reintentar refresh automáticamente:
        // - Login (obtiene tokens)
        // - Refresh (evita bucle infinito)
        // - Logout (termina sesión de forma explícita)
        return normalizedUrl.endsWith("/login") ||
            normalizedUrl.endsWith("/login/refresh") ||
            normalizedUrl.endsWith("/login/logout")
    }

    private fun forzarLogout(razon: String?) {
        // Usamos runBlocking aquí porque estamos dentro del interceptor que es síncrono para OkHttp
        runBlocking {
            sessionManager.saveLoginState(false)
            sessionManager.clear()

            val intent = Intent("com.rutai.app.FORCE_LOGOUT")
            context.sendBroadcast(intent)
            Log.d(TAG, "📡 Logout forzado enviado por: $razon")
        }
    }
}
