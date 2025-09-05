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

    @POST("rutas/{id}/finalizar")
    suspend fun finalizarRuta(
        @Path("id") rutaId: Int
    )

    @POST("rutas/{id}/cancelar")
    suspend fun cancelarRuta(
        @Path("id") rutaId: Int
    )
}
