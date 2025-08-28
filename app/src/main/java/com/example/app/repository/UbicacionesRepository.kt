package com.example.app.repository

import com.example.app.models.UbicacionUsuarioCreate
import com.example.app.models.UbicacionUsuarioResponse
import com.example.app.network.RetrofitClient
import retrofit2.HttpException
import java.io.IOException

class UbicacionesRepository {
    private val api = RetrofitClient.ubicacionesApiService

    suspend fun crearUbicacion(token: String, ubicacion: UbicacionUsuarioCreate): Result<UbicacionUsuarioResponse> {
        return try {
            val response = api.crearUbicacion("Bearer $token", ubicacion)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Respuesta vacía"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error desconocido"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Error HTTP: ${e.message}"))
        }
    }

    suspend fun obtenerUbicaciones(token: String): Result<List<UbicacionUsuarioResponse>> {
        return try {
            val response = api.obtenerUbicaciones("Bearer $token")
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Lista vacía"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error desconocido"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Error HTTP: ${e.message}"))
        }
    }
}
