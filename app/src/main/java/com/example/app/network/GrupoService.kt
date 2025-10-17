package com.example.app.network

import com.example.app.models.GrupoCreate
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GrupoService {
    @POST("grupos/crear")
    suspend fun createGrupo(
        @Body grupo: GrupoCreate,
        @Header("Authorization") token: String
    ): Response<GrupoResponse>
}

data class GrupoResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("codigo_invitacion") val codigoInvitacion: String,
    @SerializedName("creado_por_id") val creadoPorId: Int,
    @SerializedName("fecha_creacion") val fechaCreacion: String
)
