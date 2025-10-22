package com.example.app.models

import com.google.gson.annotations.SerializedName

data class MensajeResponse(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("remitente_id")
    val remitenteId: Int? = null,

    @SerializedName("remitente_nombre")
    val remitenteNombre: String? = null,

    @SerializedName("grupo_id")
    val grupoId: Int? = null,

    @SerializedName("contenido")
    val contenido: String? = null,

    @SerializedName("tipo")
    val tipo: String? = null,

    @SerializedName("fecha_creacion")
    val fechaCreacion: String? = null,

    @SerializedName("leido")
    val leido: Boolean? = null,

    @SerializedName("leido_por")
    val leidoPor: Int? = null,

    // 🔹 Campos adicionales para mensajes del sistema
    @SerializedName("type")
    val type: String? = null,

    @SerializedName("message")
    val message: String? = null
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