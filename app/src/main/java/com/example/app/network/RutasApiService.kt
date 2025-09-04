package com.example.app.network

import com.example.app.models.RutaUsuario
import retrofit2.Response
import retrofit2.http.*

interface RutasApiService {

    @POST("rutas/")
    suspend fun crearRuta(
        @Header("Authorization") token: String,
        @Body ruta: RutaUsuario
    ): Response<RutaUsuario>

    @GET("rutas/")
    suspend fun listarRutas(
        @Header("Authorization") token: String
    ): Response<List<RutaUsuario>>

    @GET("rutas/{id}")
    suspend fun obtenerRuta(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<RutaUsuario>

    @DELETE("rutas/{id}")
    suspend fun eliminarRuta(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>
}
