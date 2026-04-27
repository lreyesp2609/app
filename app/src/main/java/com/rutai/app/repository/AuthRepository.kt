package com.rutai.app.repository

import android.util.Log
import com.rutai.app.models.LoginResponse
import com.rutai.app.models.ProfileResponse
import com.rutai.app.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AuthRepository {

    private val api = RetrofitClient.apiService

    suspend fun login(
        correo: String,
        contrasenia: String,
        dispositivo: String? = null,
        versionApp: String? = null,
        ip: String? = null
    ): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.login(
                    correo = correo,
                    contrasenia = contrasenia,
                    dispositivo = dispositivo,
                    versionApp = versionApp,
                    ip = ip
                )
                if (response.isSuccessful) {
                    response.body()?.let { Result.success(it) }
                        ?: Result.failure(Exception("Respuesta vacía"))
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "INVALID_CREDENTIALS"
                        422 -> "Datos enviados incompletos o inválidos"
                        else -> response.errorBody()?.string() ?: "Error desconocido"
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: SocketTimeoutException) {
                Result.failure(Exception("NETWORK_ERROR:TIMEOUT - ${e.message}"))
            } catch (e: UnknownHostException) {
                Result.failure(Exception("NETWORK_ERROR:NO_INTERNET - ${e.message}"))
            } catch (e: IOException) {
                Result.failure(Exception("NETWORK_ERROR:IO_EXCEPTION - ${e.message}"))
            } catch (e: HttpException) {
                Result.failure(Exception("HTTP_ERROR:${e.code()} - ${e.message}"))
            } catch (e: Exception) {
                Result.failure(Exception("UNKNOWN_ERROR - ${e.message}"))
            }
        }
    }

    suspend fun getCurrentUser(token: String) = withContext(Dispatchers.IO) {
        try {
            Log.d("AuthRepository", "Obteniendo usuario con token: $token")
            val response = api.getCurrentUser(token)
            Log.d("AuthRepository", "Respuesta código: ${response.code()}")

            if (response.isSuccessful) {
                val user = response.body()
                Log.d("AuthRepository", "Usuario obtenido: $user")
                user?.let { Result.success(it) }
                    ?: Result.failure(Exception("Usuario no encontrado"))
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "AUTH_ERROR:TOKEN_INVALIDO"
                    404 -> "Usuario no encontrado"
                    else -> "HTTP_ERROR:${response.code()}"
                }
                Log.e("AuthRepository", "Error en respuesta: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: SocketTimeoutException) {
            Log.e("AuthRepository", "Timeout: ${e.message}")
            Result.failure(Exception("NETWORK_ERROR:TIMEOUT"))
        } catch (e: UnknownHostException) {
            Log.e("AuthRepository", "Sin internet: ${e.message}")
            Result.failure(Exception("NETWORK_ERROR:NO_INTERNET"))
        } catch (e: IOException) {
            Log.e("AuthRepository", "Error IO: ${e.message}")
            Result.failure(Exception("NETWORK_ERROR:IO_EXCEPTION"))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Excepción: ${e.message}")
            Result.failure(Exception("UNKNOWN_ERROR - ${e.message}"))
        }
    }

    // ✅ CRÍTICO: Refresh token con clasificación de errores
    suspend fun refreshToken(refreshToken: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.refreshToken(refreshToken)

                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("AUTH_ERROR:RESPUESTA_VACIA"))
                } else {
                    // ✅ Clasificar errores según código HTTP
                    val errorType = when (response.code()) {
                        401 -> {
                            val errorBody = response.errorBody()?.string() ?: ""
                            when {
                                errorBody.contains("REFRESH_INVALIDO") -> "AUTH_ERROR:REFRESH_INVALIDO"
                                errorBody.contains("REFRESH_EXPIRADO") -> "AUTH_ERROR:REFRESH_EXPIRADO"
                                else -> "AUTH_ERROR:UNAUTHORIZED"
                            }
                        }
                        403 -> "AUTH_ERROR:FORBIDDEN"
                        404 -> "AUTH_ERROR:SESION_NO_ENCONTRADA"
                        500, 502, 503, 504 -> "SERVER_ERROR:${response.code()}"
                        else -> "HTTP_ERROR:${response.code()}"
                    }

                    Result.failure(Exception(errorType))
                }
            } catch (e: SocketTimeoutException) {
                // ✅ Error de timeout - NO debe causar logout inmediato
                Result.failure(Exception("NETWORK_ERROR:TIMEOUT"))
            } catch (e: UnknownHostException) {
                // ✅ Sin conexión a internet - NO debe causar logout inmediato
                Result.failure(Exception("NETWORK_ERROR:NO_INTERNET"))
            } catch (e: IOException) {
                // ✅ Error de red genérico - NO debe causar logout inmediato
                Result.failure(Exception("NETWORK_ERROR:IO_EXCEPTION"))
            } catch (e: HttpException) {
                // ✅ Error HTTP específico
                Result.failure(Exception("HTTP_ERROR:${e.code()}"))
            } catch (e: Exception) {
                // ✅ Error desconocido
                Result.failure(Exception("UNKNOWN_ERROR:${e.message}"))
            }
        }
    }

    suspend fun logout(refreshToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.logout(refreshToken)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: SocketTimeoutException) {
                Result.failure(Exception("NETWORK_ERROR:TIMEOUT"))
            } catch (e: UnknownHostException) {
                Result.failure(Exception("NETWORK_ERROR:NO_INTERNET"))
            } catch (e: IOException) {
                Result.failure(Exception("NETWORK_ERROR:IO_EXCEPTION"))
            } catch (e: HttpException) {
                Result.failure(Exception("HTTP_ERROR:${e.code()}"))
            } catch (e: Exception) {
                Result.failure(Exception("UNKNOWN_ERROR - ${e.message}"))
            }
        }
    }

    suspend fun register(
        nombre: String,
        apellido: String,
        correo: String,
        contrasenia: String
    ): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.register(
                    nombre = nombre,
                    apellido = apellido,
                    correo = correo,
                    contrasenia = contrasenia
                )
                if (response.isSuccessful) {
                    response.body()?.let { Result.success(it) }
                        ?: Result.failure(Exception("Respuesta vacía"))
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "USER_ALREADY_EXISTS"
                        422 -> "INVALID_EMAIL"
                        else -> response.errorBody()?.string() ?: "Error desconocido"
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: SocketTimeoutException) {
                Result.failure(Exception("NETWORK_ERROR:TIMEOUT"))
            } catch (e: UnknownHostException) {
                Result.failure(Exception("NETWORK_ERROR:NO_INTERNET"))
            } catch (e: IOException) {
                Result.failure(Exception("NETWORK_ERROR:IO_EXCEPTION"))
            } catch (e: Exception) {
                Result.failure(Exception("UNKNOWN_ERROR - ${e.message}"))
            }
        }
    }

    // 🔥 Métodos FCM
    suspend fun enviarTokenFCM(bearerToken: String, request: Map<String, String>): Response<Unit> {
        return withContext(Dispatchers.IO) {
            api.registrarFCMToken(bearerToken, request)
        }
    }

    suspend fun eliminarTokenFCM(bearerToken: String): Response<Unit> {
        return withContext(Dispatchers.IO) {
            api.eliminarTodosLosTokens(bearerToken)
        }
    }

    suspend fun actualizarPerfil(
        token: String,
        nombre: String,
        apellido: String
    ): Result<ProfileResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.actualizarPerfil(
                    token = "Bearer $token",
                    nombre = nombre,
                    apellido = apellido
                )
                if (response.isSuccessful) {
                    response.body()?.let { Result.success(it) }
                        ?: Result.failure(Exception("Respuesta vacía"))
                } else {
                    Result.failure(Exception("Error ${response.code()}"))
                }
            } catch (e: IOException) {
                Result.failure(Exception("NETWORK_ERROR"))
            } catch (e: Exception) {
                Result.failure(Exception("UNKNOWN_ERROR - ${e.message}"))
            }
        }
    }
}