package com.rutai.app.repository

import com.rutai.app.models.EstadisticasResponse
import com.rutai.app.models.FinalizarRutaRequest
import com.rutai.app.models.FinalizarRutaResponse
import com.rutai.app.models.PuntoGPS
import com.rutai.app.models.RutaUsuario
import com.rutai.app.models.ValidarRutasRequest
import com.rutai.app.models.ValidarRutasResponse
import com.rutai.app.models.ZonaPeligrosaCreate
import com.rutai.app.models.ZonaPeligrosaResponse
import com.rutai.app.network.RetrofitClient
import com.rutai.app.utils.safeApiCall

class RutasRepository {
    private val api = RetrofitClient.rutasApiService
    private val mlApi = RetrofitClient.mlService

    suspend fun guardarRuta(token: String, ruta: RutaUsuario): Result<RutaUsuario> {
        return safeApiCall { api.crearRuta("Bearer $token", ruta) }
    }

    suspend fun obtenerEstadisticas(token: String, ubicacionId: Int): Result<EstadisticasResponse> {
        return try {
            val response = mlApi.obtenerMisEstadisticas("Bearer $token", ubicacionId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🔥 Cancelar ruta - ahora directamente con Query
    suspend fun cancelarRuta(rutaId: Int, fechaFin: String): Result<Unit> {
        return safeApiCall { api.cancelarRuta(rutaId, fechaFin) }
    }

    // 🔥 Finalizar ruta con puntos GPS y análisis
    suspend fun finalizarRuta(
        rutaId: Int,
        fechaFin: String,
        puntosGPS: List<PuntoGPS>? = null,
        siguioRutaRecomendada: Boolean? = null,
        porcentajeSimilitud: Double? = null
    ): Result<FinalizarRutaResponse> {
        return try {
            val request = FinalizarRutaRequest(
                fecha_fin = fechaFin,
                puntos_gps = puntosGPS,
                siguio_ruta_recomendada = siguioRutaRecomendada,
                porcentaje_similitud = porcentajeSimilitud
            )
            Result.success(mlApi.finalizarRuta(rutaId, request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Seguridad
    suspend fun marcarZonaPeligrosa(token: String, zona: ZonaPeligrosaCreate): Result<ZonaPeligrosaResponse> {
        return try {
            Result.success(api.marcarZonaPeligrosa("Bearer $token", zona))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerMisZonasPeligrosas(token: String, activasSolo: Boolean = true): Result<List<ZonaPeligrosaResponse>> {
        return try {
            Result.success(api.obtenerMisZonasPeligrosas("Bearer $token", activasSolo))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validarRutas(token: String, request: ValidarRutasRequest): Result<ValidarRutasResponse> {
        return try {
            Result.success(api.validarRutas("Bearer $token", request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarZonaPeligrosa(token: String, zonaId: Int): Result<Unit> {
        return safeApiCall { api.eliminarZonaPeligrosa("Bearer $token", zonaId) }
    }

    suspend fun obtenerZonasSugeridas(token: String, lat: Double, lon: Double, radioKm: Float = 10.0f): Result<List<ZonaPeligrosaResponse>> {
        return try {
            Result.success(api.obtenerZonasSugeridas("Bearer $token", lat, lon, radioKm))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adoptarZonaSugerida(token: String, zonaId: Int): Result<ZonaPeligrosaResponse> {
        return try {
            Result.success(api.adoptarZonaSugerida("Bearer $token", zonaId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
