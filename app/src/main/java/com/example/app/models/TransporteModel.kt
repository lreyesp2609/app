package com.example.app.models

data class Transporte(
    val nombre: String,
    val descripcion: String? = null
)

data class PasoRuta(
    val instruccion: String,
    val distancia: Double,
    val duracion: Double,
    val tipo: Int? = null,
    val id: Int? = null,
    val segmento_id: Int? = null
)

data class SegmentoRuta(
    val distancia: Double,
    val duracion: Double,
    val pasos: List<PasoRuta> = emptyList(),
    val id: Int? = null,
    val ruta_id: Int? = null
)

data class RutaUsuario(
    val transporte_texto: String,
    val ubicacion_id: Int,
    val distancia_total: Double,
    val duracion_total: Double,
    val geometria: String,
    val fecha_inicio: String,
    val fecha_fin: String? = null,
    val segmentos: List<SegmentoRuta> = emptyList(),
    val id: Int? = null,
    val estado_id: Int? = null,
    val transporte: Transporte? = null,
    val tipo_ruta_usado: String? = null
)