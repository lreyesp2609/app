package com.example.app.network

import com.example.app.models.MarcarLeidoResponse
import com.example.app.models.MensajeResponse
import retrofit2.Response
import retrofit2.http.*

interface MensajesApiService {

    /**
     * Obtiene los mensajes de un grupo
     * GET /grupos/{grupo_id}/mensajes
     */
    @GET("grupos/{grupo_id}/mensajes")
    suspend fun obtenerMensajesGrupo(
        @Path("grupo_id") grupoId: Int,
        @Query("limit") limit: Int = 50,
        @Header("Authorization") token: String
    ): Response<List<MensajeResponse>>

    /**
     * Marca un mensaje como leído
     * POST /grupos/{grupo_id}/mensajes/{mensaje_id}/marcar-leido
     */
    @POST("grupos/{grupo_id}/mensajes/{mensaje_id}/marcar-leido")
    suspend fun marcarMensajeLeido(
        @Path("grupo_id") grupoId: Int,
        @Path("mensaje_id") mensajeId: Int,
        @Header("Authorization") token: String
    ): Response<MarcarLeidoResponse>
}