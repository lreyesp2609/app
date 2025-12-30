package com.example.app.network

import com.example.app.services.LotePuntosGPSRequest
import com.example.app.services.LotePuntosGPSResponse
import retrofit2.http.*

interface TrackingApiService {

    @POST("tracking/gps/lote")
    suspend fun guardarLotePuntosGPS(
        @Header("Authorization") token: String,
        @Body request: LotePuntosGPSRequest
    ): LotePuntosGPSResponse

    @GET("tracking/viajes")
    suspend fun obtenerMisViajes(
        @Header("Authorization") token: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("ubicacion_id") ubicacionId: Int? = null
    ): List<ViajeDetectadoResponse>

    @GET("tracking/patrones")
    suspend fun obtenerMisPatrones(
        @Header("Authorization") token: String
    ): List<PatronPredictibilidadResponse>

    @GET("tracking/estadisticas")
    suspend fun obtenerEstadisticasTracking(
        @Header("Authorization") token: String
    ): EstadisticasTrackingResponse
}

// ════════════════════════════════════════════════════════════
// 📦 DATA CLASSES
// ════════════════════════════════════════════════════════════

data class ViajeDetectadoResponse(
    val id: Int,
    val usuario_id: Int,
    val ubicacion_origen_id: Int?,
    val ubicacion_destino_id: Int?,
    val lat_inicio: Double,
    val lon_inicio: Double,
    val lat_fin: Double,
    val lon_fin: Double,
    val fecha_inicio: String,
    val fecha_fin: String,
    val distancia_metros: Double,
    val duracion_segundos: Int
)

data class PatronPredictibilidadResponse(
    val id: Int,
    val usuario_id: Int,
    val ubicacion_destino_id: Int,
    val total_viajes: Int,
    val viajes_ruta_similar: Int,
    val predictibilidad: Double,
    val es_predecible: Boolean,
    val notificacion_enviada: Boolean,
    val predictibilidad_porcentaje: Int,
    val nivel_riesgo: String
)

data class EstadisticasTrackingResponse(
    val total_viajes: Int,
    val viajes_este_mes: Int,
    val distancia_total_km: Double,
    val total_patrones: Int,
    val patrones_predecibles: Int,
    val puntos_gps_este_mes: Int,
    val promedio_viajes_dia: Double,
    val porcentaje_predictibilidad: Double
)