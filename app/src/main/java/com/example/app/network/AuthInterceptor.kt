package com.example.app.network

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.app.repository.AuthRepository
import com.example.app.utils.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 🔐 AuthInterceptor - Refresca el token automáticamente cuando expira
 *
 * Flujo:
 * 1. Detecta respuesta 401 (Token expirado)
 * 2. Intenta refrescar el token usando refresh_token
 * 3. Si el refresh es exitoso, reintenta el request original
 * 4. Si el refresh falla, fuerza logout
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    private val sessionManager by lazy { SessionManager.getInstance(context) }
    private val authRepository by lazy { AuthRepository() }

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1️⃣ Ejecutar request original
        var response = chain.proceed(originalRequest)

        // 2️⃣ Si es 401 Y NO es el endpoint de refresh/login, intentar refrescar token
        if (response.code == 401 && !isAuthEndpoint(originalRequest.url.toString())) {
            Log.w(TAG, "⚠️ ========================================")
            Log.w(TAG, "⚠️ 401 DETECTADO - Token expirado")
            Log.w(TAG, "⚠️ URL: ${originalRequest.url}")
            Log.w(TAG, "⚠️ ========================================")

            // 🔥 NO CERRAR response.close() - Dejar que OkHttp lo maneje

            // Intentar refresh (sincronizado para evitar múltiples refreshes simultáneos)
            synchronized(this) {
                val refreshResult = intentarRefreshToken()

                if (refreshResult.isSuccess) {
                    val nuevoAccessToken = refreshResult.getOrNull()

                    if (nuevoAccessToken != null) {
                        Log.d(TAG, "✅ ========================================")
                        Log.d(TAG, "✅ TOKEN REFRESCADO EXITOSAMENTE")
                        Log.d(TAG, "✅ Reintentando request original...")
                        Log.d(TAG, "✅ ========================================")

                        // 🔥 Ahora SÍ cerramos la respuesta 401 porque vamos a crear una nueva
                        response.close()

                        // Crear nueva request con token actualizado
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $nuevoAccessToken")
                            .build()

                        // Reintentar request original con nuevo token
                        response = chain.proceed(newRequest)
                    }
                } else {
                    Log.e(TAG, "❌ ========================================")
                    Log.e(TAG, "❌ ERROR AL REFRESCAR TOKEN")
                    Log.e(TAG, "❌ ${refreshResult.exceptionOrNull()?.message}")
                    Log.e(TAG, "❌ Forzando logout...")
                    Log.e(TAG, "❌ ========================================")

                    // Si el refresh falló, limpiar sesión
                    forzarLogout(refreshResult.exceptionOrNull()?.message)

                    // Retornar la respuesta 401 sin modificar
                    // (OkHttp se encargará de cerrarla)
                }
            }
        }

        return response
    }

    /**
     * Intenta refrescar el access token usando el refresh token
     */
    private fun intentarRefreshToken(): Result<String?> {
        return runBlocking {
            try {
                val refreshToken = sessionManager.getRefreshToken()

                if (refreshToken.isNullOrEmpty()) {
                    Log.e(TAG, "❌ No hay refresh token disponible")
                    return@runBlocking Result.failure(Exception("NO_REFRESH_TOKEN"))
                }

                Log.d(TAG, "🔄 Intentando refrescar token...")
                Log.d(TAG, "   Refresh token: ${refreshToken.take(20)}...")

                // Llamar al endpoint de refresh
                val result = authRepository.refreshToken(refreshToken)

                if (result.isSuccess) {
                    val loginResponse = result.getOrNull()

                    if (loginResponse != null) {
                        // ✅ Guardar nuevos tokens
                        sessionManager.saveTokens(
                            access = loginResponse.accessToken,
                            refresh = loginResponse.refreshToken
                        )

                        Log.d(TAG, "✅ Tokens guardados correctamente")
                        Log.d(TAG, "   Nuevo access: ${loginResponse.accessToken.take(20)}...")
                        Log.d(TAG, "   Nuevo refresh: ${loginResponse.refreshToken.take(20)}...")

                        Result.success(loginResponse.accessToken)
                    } else {
                        Log.e(TAG, "❌ Respuesta de refresh vacía")
                        Result.failure(Exception("REFRESH_RESPONSE_NULL"))
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "UNKNOWN_ERROR"
                    Log.e(TAG, "❌ Error en refresh: $error")

                    // Clasificar error para decidir si hacer logout
                    when {
                        error.contains("AUTH_ERROR:REFRESH_INVALIDO") ||
                                error.contains("AUTH_ERROR:REFRESH_EXPIRADO") ||
                                error.contains("AUTH_ERROR:SESION_NO_ENCONTRADA") -> {
                            Log.e(TAG, "🚪 Error de autenticación - requiere logout")
                            Result.failure(Exception("FORCE_LOGOUT:$error"))
                        }
                        error.contains("NETWORK_ERROR") ||
                                error.contains("SERVER_ERROR") -> {
                            Log.w(TAG, "⚠️ Error temporal, no forzar logout")
                            Result.failure(Exception("TEMPORARY_ERROR:$error"))
                        }
                        else -> {
                            Log.e(TAG, "❌ Error desconocido en refresh")
                            Result.failure(Exception(error))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción al refrescar token: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    /**
     * Verifica si el request es hacia endpoints de autenticación
     * (para evitar loops infinitos)
     */
    private fun isAuthEndpoint(url: String): Boolean {
        return url.contains("/login/refresh") ||
                url.contains("/login/") && !url.contains("/login/logout")
    }

    /**
     * Fuerza logout y limpia la sesión
     */
    private fun forzarLogout(razon: String?) {
        runBlocking {
            try {
                Log.w(TAG, "🚪 ========================================")
                Log.w(TAG, "🚪 FORZANDO LOGOUT")
                Log.w(TAG, "🚪 ========================================")
                Log.w(TAG, "   Razón: ${razon ?: "Token inválido"}")

                // Limpiar sesión local
                sessionManager.saveLoginState(false)
                sessionManager.clear()

                Log.d(TAG, "✅ Sesión limpiada")
                Log.d(TAG, "ℹ️  El usuario deberá iniciar sesión nuevamente")
                Log.d(TAG, "✅ ========================================")

                // ✅ Enviar broadcast para navegar a login
                try {
                    val intent = Intent("com.example.app.FORCE_LOGOUT")
                    context.sendBroadcast(intent)
                    Log.d(TAG, "📡 Broadcast de logout enviado")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ No se pudo enviar broadcast: ${e.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al forzar logout: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}