package com.example.app.network

import com.example.app.models.UbicacionUsuarioCreate
import com.example.app.models.UbicacionUsuarioResponse
import retrofit2.Response
import retrofit2.http.*

interface UbicacionesApiService {

    @POST("ubicaciones/")
    suspend fun crearUbicacion(
        @Header("Authorization") token: String,
        @Body ubicacion: UbicacionUsuarioCreate
    ): Response<UbicacionUsuarioResponse>

    @GET("ubicaciones/")
    suspend fun obtenerUbicaciones(
        @Header("Authorization") token: String
    ): Response<List<UbicacionUsuarioResponse>>

    @GET("ubicaciones/{id}")
    suspend fun obtenerUbicacionPorId(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<UbicacionUsuarioResponse>

    @DELETE("ubicaciones/{id}")
    suspend fun eliminarUbicacion(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<UbicacionUsuarioResponse>
}
