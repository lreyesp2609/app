package com.example.app.repository

import com.example.app.models.EstadisticasResponse
import com.example.app.models.RutaUsuario
import com.example.app.network.RetrofitClient
import retrofit2.HttpException
import java.io.IOException

class RutasRepository {
    private val api = RetrofitClient.rutasApiService
    private val mlApi = RetrofitClient.mlService
    suspend fun guardarRuta(token: String, ruta: RutaUsuario): Result<RutaUsuario> {
        return try {
            val response = api.crearRuta("Bearer $token", ruta)
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

    suspend fun obtenerEstadisticas(token: String, ubicacionId: Int): Result<EstadisticasResponse> {
        return try {
            val response = mlApi.obtenerMisEstadisticas("Bearer $token", ubicacionId) // 👈 corregido
            Result.success(response)
        } catch (e: IOException) {
            Result.failure(Exception("Error de red: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Error HTTP: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error desconocido: ${e.message}"))
        }
    }


    // 🔥 Ahora con fechaFin
    suspend fun cancelarRuta(rutaId: Int, fechaFin: String) {
        api.cancelarRuta(rutaId, fechaFin)
    }

    // 🔥 Ahora con fechaFin
    suspend fun finalizarRuta(rutaId: Int, fechaFin: String) {
        api.finalizarRuta(rutaId, fechaFin)
    }


}
