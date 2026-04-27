package com.rutai.app.repository

import android.util.Log
import com.rutai.app.models.EstadisticasResponse
import com.rutai.app.models.FinalizarRutaRequest
import com.rutai.app.models.FinalizarRutaResponse
import com.rutai.app.models.PuntoGPS
import com.rutai.app.models.RutaUsuario
import com.rutai.app.network.RetrofitClient
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
            val response = mlApi.obtenerMisEstadisticas("Bearer $token", ubicacionId)
            Result.success(response)
        } catch (e: IOException) {
            Result.failure(Exception("Error de red: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Error HTTP: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error desconocido: ${e.message}"))
        }
    }

    // 🔥 Cancelar ruta - ahora directamente con Query
    suspend fun cancelarRuta(rutaId: Int, fechaFin: String) {
        api.cancelarRuta(rutaId, fechaFin)
    }

    // 🔥 Finalizar ruta con puntos GPS y análisis
    suspend fun finalizarRuta(
        rutaId: Int,
        fechaFin: String,
        puntosGPS: List<PuntoGPS>? = null,
        siguioRutaRecomendada: Boolean? = null,
        porcentajeSimilitud: Double? = null // Nuevo parámetro
    ): Result<FinalizarRutaResponse> {
        return try {
            val request = FinalizarRutaRequest(
                fecha_fin = fechaFin,
                puntos_gps = puntosGPS,
                siguio_ruta_recomendada = siguioRutaRecomendada,
                porcentaje_similitud = porcentajeSimilitud
            )

            val response = mlApi.finalizarRuta(rutaId, request)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("RutasRepository", "Error finalizando ruta: ${e.message}", e)
            Result.failure(e)
        }
    }
}