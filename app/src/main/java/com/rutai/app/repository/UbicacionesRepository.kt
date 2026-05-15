package com.rutai.app.repository

import android.util.Log
import com.rutai.app.models.UbicacionUsuarioCreate
import com.rutai.app.models.UbicacionUsuarioResponse
import com.rutai.app.network.RetrofitClient
import retrofit2.HttpException
import java.io.IOException

class UbicacionesRepository {
    private val api = RetrofitClient.ubicacionesApiService
    private val TAG = "UbicacionesRepository"

    suspend fun crearUbicacion(token: String, ubicacion: UbicacionUsuarioCreate): Result<UbicacionUsuarioResponse> {
        return try {
            val response = api.crearUbicacion("Bearer $token", ubicacion)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("EMPTY_RESPONSE"))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "UNKNOWN_ERROR"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Result.failure(Exception("NETWORK_ERROR"))
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP_ERROR_${e.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in crearUbicacion: ${e.message}")
            Result.failure(e)
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
            Result.failure(Exception("HTTP_ERROR_${e.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in obtenerUbicaciones: ${e.message}")
            Result.failure(e)
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
            Result.failure(Exception("HTTP_ERROR_${e.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in obtenerUbicacionPorId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun eliminarUbicacion(token: String, id: Int): Result<UbicacionUsuarioResponse> {
        return try {
            val response = api.eliminarUbicacion("Bearer $token", id)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("EMPTY_RESPONSE"))
            } else {
                Result.failure(Exception("HTTP_ERROR_${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in eliminarUbicacion: ${e.message}")
            Result.failure(e)
        }
    }
}
