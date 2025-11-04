package com.example.app.models

import com.google.gson.annotations.SerializedName

data class GrupoCreate(
    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String? = null
)

data class GrupoResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("codigo_invitacion") val codigoInvitacion: String,
    @SerializedName("creado_por_id") val creadoPorId: Int,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    @SerializedName("mensajes_no_leidos") val mensajesNoLeidos: Int = 0
)