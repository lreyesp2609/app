package com.example.app.network

import com.example.app.models.GrupoCreate
import com.example.app.models.GrupoResponseSalir
import com.example.app.models.IntegrantesResponse
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
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

}

data class GrupoResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("codigo_invitacion") val codigoInvitacion: String,
    @SerializedName("creado_por_id") val creadoPorId: Int,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    @SerializedName("mensajes_no_leidos") val mensajesNoLeidos: Int = 0
)