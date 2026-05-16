package com.rutai.app.network

import android.content.Context
import android.content.Intent
import android.util.Log
import com.rutai.app.BuildConfig
import com.rutai.app.utils.SessionManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 🔐 AuthInterceptor - Gestiona la autenticación y el refresco de tokens.
 *
 * Cambios realizados:
 * 1. Cliente aislado: Se usa un OkHttpClient diferente para el refresh, evitando deadlocks.
 * 2. Inyección de Token: Añade automáticamente "Bearer <token>" a las peticiones.
 * 3. Mutex Sincronizado: Asegura que si 10 peticiones fallan a la vez, solo se haga 1 refresh.
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    private val sessionManager by lazy { SessionManager.getInstance(context) }

    // 🚀 Cliente y servicio dedicados para el refresco (Aislado del cliente principal)
    private val refreshApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    companion object {
        private const val TAG = "AuthInterceptor"
        private val mutex = Mutex()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1️⃣ Obtener el token actual
        val token = sessionManager.getAccessToken()

        // 2️⃣ Añadir header de Authorization si existe y no está ya presente
        val requestBuilder = originalRequest.newBuilder()
        if (token != null && originalRequest.header("Authorization") == null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val currentRequest = requestBuilder.build()

        // 3️⃣ Ejecutar la petición
        var response = chain.proceed(currentRequest)

        // 4️⃣ Si es 401 y no es un endpoint de login/refresh, intentamos refrescar
        if (response.code == 401 && !isAuthEndpoint(currentRequest.url.toString())) {
            Log.w(TAG, "⚠️ 401 Detectado en ${currentRequest.url}. Iniciando refresh sincronizado...")

            val refreshResult = runBlocking {
                mutex.withLock {
                    val tokenActual = sessionManager.getAccessToken()
                    val tokenEnRequest = currentRequest.header("Authorization")?.removePrefix("Bearer ")

                    // Si el token ya cambió, otra petición concurrente ya hizo el refresh con éxito
                    if (tokenActual != null && tokenActual != tokenEnRequest) {
                        Log.d(TAG, "🔄 Token ya refrescado por otra petición")
                        Result.success(tokenActual)
                    } else {
                        ejecutarRefreshSeguro()
                    }
                }
            }

            if (refreshResult.isSuccess) {
                val nuevoToken = refreshResult.getOrNull()
                if (nuevoToken != null) {
                    response.close() // Cerrar la respuesta 401 anterior

                    val retryRequest = currentRequest.newBuilder()
                        .header("Authorization", "Bearer $nuevoToken")
                        .build()

                    // Reintentar la petición original con el nuevo token
                    return chain.proceed(retryRequest)
                }
            } else {
                val error = refreshResult.exceptionOrNull()?.message
                Log.e(TAG, "❌ Falló el refresh automático: $error. Forzando logout.")
                forzarLogout(error)
            }
        }

        return response
    }

    /**
     * Realiza la llamada al servidor para obtener un nuevo par de tokens.
     */
    private suspend fun ejecutarRefreshSeguro(): Result<String> {
        return try {
            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                return Result.failure(Exception("NO_REFRESH_TOKEN_AVAILABLE"))
            }

            val response = refreshApiService.refreshToken(refreshToken)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Guardamos tokens. SessionManager calcula ahora el tiempo de expiración.
                    sessionManager.saveTokens(
                        access = body.accessToken,
                        refresh = body.refreshToken
                    )
                    Result.success(body.accessToken)
                } else {
                    Result.failure(Exception("REFRESH_RESPONSE_EMPTY"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown"
                Result.failure(Exception("REFRESH_FAILED: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔥 Excepción en el proceso de refresh: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Evita bucles infinitos no intentando refrescar en endpoints de auth.
     */
    private fun isAuthEndpoint(url: String): Boolean {
        val normalizedUrl = url.substringBefore("?").trimEnd('/')
        return normalizedUrl.endsWith("/login") ||
                normalizedUrl.endsWith("/login/refresh") ||
                normalizedUrl.endsWith("/login/logout") ||
                normalizedUrl.contains("/usuarios/registrar")
    }

    /**
     * Limpia la sesión local y notifica a la UI para redirigir al Login.
     */
    private fun forzarLogout(razon: String?) {
        runBlocking {
            sessionManager.saveLoginState(false)
            sessionManager.clear()
            sessionManager.stopAutoRefresh()

            val intent = Intent("com.rutai.app.FORCE_LOGOUT").apply {
                setPackage(context.packageName)
                putExtra("reason", razon)
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "📡 Logout forzado ejecutado")
        }
    }
}
