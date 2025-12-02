package com.example.app.network

import com.example.app.models.GrupoCreate
import com.example.app.models.GrupoDeleteResponse
import com.example.app.models.GrupoResponse
import com.example.app.models.GrupoResponseSalir
import com.example.app.models.IntegrantesResponse
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GrupoService {
    @POST("grupos/crear")
    suspend fun createGrupo(
        @Body grupo: GrupoCreate,
        @Header("Authorization") token: String
    ): Response<GrupoResponse>

    @GET("grupos/listar")
    suspend fun listarGrupos(
        @Header("Authorization") token: String
    ): Response<List<GrupoResponse>>

    @GET("grupos/grupos-con-no-leidos")
    suspend fun listarGruposConNoLeidos(
        @Header("Authorization") token: String
    ): Response<List<GrupoResponse>>

    @POST("grupos/unirse/{codigo}")
    suspend fun unirseAGrupo(
        @Path("codigo") codigo: String,
        @Header("Authorization") token: String
    ): Response<GrupoResponse>

    @GET("grupos/{grupo_id}/integrantes")
    suspend fun obtenerIntegrantes(
        @Path("grupo_id") grupoId: Int,
        @Header("Authorization") token: String
    ): Response<IntegrantesResponse>

    @POST("grupos/{grupo_id}/salir")
    suspend fun salirDelGrupo(
        @Path("grupo_id") grupoId: Int,
        @Header("Authorization") token: String
    ): Response<GrupoResponseSalir>

    @DELETE("grupos/eliminar/{grupo_id}")
    suspend fun eliminarGrupo(
        @Path("grupo_id") grupoId: Int,
        @Header("Authorization") token: String
    ): Response<GrupoDeleteResponse>


    @POST("grupos/{grupoId}/mensajes/marcar-entregados")
    suspend fun marcarMensajesEntregados(
        @Header("Authorization") token: String,
        @Path("grupoId") grupoId: Int
    ): Response<JsonObject>

}