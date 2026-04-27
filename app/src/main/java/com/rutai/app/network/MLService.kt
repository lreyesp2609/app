package com.rutai.app.network

import com.rutai.app.models.*
import retrofit2.http.*

interface MLService {

    @POST("ml/recomendar-tipo-ruta")
    suspend fun getRecomendacionTipoRuta(
        @Header("Authorization") token: String,
        @Body body: TipoRutaRequest
    ): TipoRutaResponse

    @POST("ml/feedback-ruta")
    suspend fun enviarFeedback(
        @Header("Authorization") token: String,
        @Body feedback: FeedbackRequest
    ): FeedbackResponse

    @GET("ml/stats")
    suspend fun obtenerMisEstadisticas(
        @Header("Authorization") token: String,
        @Query("ubicacion_id") ubicacionId: Int
    ): EstadisticasResponse

    // 🔥 AGREGAR ESTE ENDPOINT:
    @POST("rutas/{id}/finalizar")
    suspend fun finalizarRuta(
        @Path("id") rutaId: Int,
        @Body request: FinalizarRutaRequest
    ): FinalizarRutaResponse
}