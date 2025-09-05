package com.example.app.network

import com.example.app.models.EstadisticasResponse
import com.example.app.models.FeedbackRequest
import com.example.app.models.FeedbackResponse
import com.example.app.models.TipoRutaResponse
import retrofit2.http.*

interface MLService {
    @GET("ml/recomendar-tipo-ruta")
    suspend fun getRecomendacionTipoRuta(
        @Header("Authorization") token: String  // JWT en header
    ): TipoRutaResponse

    @POST("ml/feedback-ruta")
    suspend fun enviarFeedback(
        @Header("Authorization") token: String,  // JWT en header
        @Body feedback: FeedbackRequest
    ): FeedbackResponse

    @GET("ml/stats")
    suspend fun obtenerMisEstadisticas(
        @Header("Authorization") token: String
    ): EstadisticasResponse
}