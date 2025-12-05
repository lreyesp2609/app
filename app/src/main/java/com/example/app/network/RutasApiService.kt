package com.example.app.network

import com.example.app.models.EstadisticasSeguridad
import com.example.app.models.FinalizarRutaRequest
import com.example.app.models.FinalizarRutaResponse
import com.example.app.models.RutaUsuario
import com.example.app.models.ValidarRutasRequest
import com.example.app.models.ValidarRutasResponse
import com.example.app.models.ZonaPeligrosaCreate
import com.example.app.models.ZonaPeligrosaResponse
import retrofit2.Response
import retrofit2.http.*

interface RutasApiService {

    @POST("rutas/")
    suspend fun crearRuta(
        @Header("Authorization") token: String,
        @Body ruta: RutaUsuario
    ): Response<RutaUsuario>

    @POST("rutas/{id}/cancelar")
    suspend fun cancelarRuta(
        @Path("id") rutaId: Int,
        @Query("fecha_fin") fechaFin: String
    ): Response<Unit>


    // Seguridad

    @POST("seguridad/marcar-zona")
    suspend fun marcarZonaPeligrosa(
        @Header("Authorization") token: String,
        @Body zona: ZonaPeligrosaCreate
    ): ZonaPeligrosaResponse

    @GET("seguridad/mis-zonas")
    suspend fun obtenerMisZonasPeligrosas(
        @Header("Authorization") token: String,
        @Query("activas_solo") activasSolo: Boolean = true
    ): List<ZonaPeligrosaResponse>

    @POST("seguridad/validar-rutas")
    suspend fun validarRutas(
        @Header("Authorization") token: String,
        @Body request: ValidarRutasRequest
    ): ValidarRutasResponse

    @DELETE("seguridad/zona/{zona_id}")
    suspend fun eliminarZonaPeligrosa(
        @Header("Authorization") token: String,
        @Path("zona_id") zonaId: Int
    ): Response<Unit>

    @PATCH("seguridad/zona/{zona_id}/toggle")
    suspend fun toggleZonaActiva(
        @Header("Authorization") token: String,
        @Path("zona_id") zonaId: Int
    ): Response<Map<String, Any>>

    @GET("seguridad/estadisticas")
    suspend fun obtenerEstadisticasSeguridad(
        @Header("Authorization") token: String
    ): EstadisticasSeguridad

}
