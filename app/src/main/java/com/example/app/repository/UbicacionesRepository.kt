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
                    ?: Result.failure(Exception("EMPTY_RESPONSE"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "UNKNOWN_ERROR"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("NETWORK_ERROR"))
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP_ERROR"))
        }
    }

    suspend fun obtenerUbicaciones(token: String): Result<List<UbicacionUsuarioResponse>> {
        return try {
            val response = api.obtenerUbicaciones("Bearer $token")
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("EMPTY_LIST"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "UNKNOWN_ERROR"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("NETWORK_ERROR"))
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP_ERROR"))
        }
    }

    suspend fun obtenerUbicacionPorId(token: String, id: Int): Result<UbicacionUsuarioResponse> {
        return try {
            val response = api.obtenerUbicacionPorId("Bearer $token", id)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("NOT_FOUND"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "UNKNOWN_ERROR"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("NETWORK_ERROR"))
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP_ERROR"))
        }
    }

    suspend fun eliminarUbicacion(token: String, id: Int): Result<UbicacionUsuarioResponse> {
        return try {
            val response = api.eliminarUbicacion("Bearer $token", id)

            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("HTTP_ERROR_${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("NETWORK_ERROR"))
        }
    }

}
