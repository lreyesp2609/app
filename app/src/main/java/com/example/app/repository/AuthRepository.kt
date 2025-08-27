package com.example.app.repository

import android.util.Log
import com.example.app.models.LoginResponse
import com.example.app.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

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
                        401 -> "Usuario o contraseña incorrectos"
                        422 -> "Datos enviados incompletos o inválidos"
                        else -> response.errorBody()?.string() ?: "Error desconocido"
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: IOException) {
                Result.failure(Exception("Error de red: ${e.message}"))
            } catch (e: HttpException) {
                Result.failure(Exception("Error HTTP: ${e.message}"))
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
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                Log.e("AuthRepository", "Error en respuesta: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Excepción: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun refreshToken(refreshToken: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.refreshToken(refreshToken)
                if (response.isSuccessful) {
                    response.body()?.let { Result.success(it) }
                        ?: Result.failure(Exception("Respuesta vacía en refresh"))
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
