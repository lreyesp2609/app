package com.example.app.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo que representa un mensaje del grupo
 * Corresponde a MensajeOut del backend
 */
data class MensajeResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("remitente_id")
    val remitenteId: Int,

    @SerializedName("remitente_nombre")
    val remitenteNombre: String?,

    @SerializedName("grupo_id")
    val grupoId: Int,

    @SerializedName("contenido")
    val contenido: String,

    @SerializedName("tipo")
    val tipo: String,

    @SerializedName("fecha_creacion")
    val fechaCreacion: String,

    @SerializedName("leido")
    val leido: Boolean,

    @SerializedName("leido_por")
    val leidoPor: Int
)

/**
 * Respuesta al marcar un mensaje como leído
 */
data class MarcarLeidoResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("leido")
    val leido: Boolean
)

/**
 * Modelo para la UI (convertido desde MensajeResponse)
 */
data class MensajeUI(
    val id: Int,
    val contenido: String,
    val esMio: Boolean,
    val hora: String, // Formateado para mostrar (ej: "10:30")
    val leido: Boolean,
    val leidoPor: Int,
    val nombreRemitente: String?,
    val remitenteId: Int,
    val tipo: String,
    val fechaCreacion: String
)
