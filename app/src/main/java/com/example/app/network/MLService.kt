package com.example.app.network

import com.example.app.models.EstadisticasResponse
import com.example.app.models.FeedbackRequest
import com.example.app.models.FeedbackResponse
import com.example.app.models.TipoRutaRequest
import com.example.app.models.TipoRutaResponse
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
}
