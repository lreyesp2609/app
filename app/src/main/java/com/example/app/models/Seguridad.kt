package com.example.app.models

import com.google.gson.annotations.SerializedName

data class ZonaPeligrosaCreate(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("radio_metros") val radioMetros: Int = 200,
    @SerializedName("nivel_peligro") val nivelPeligro: Int = 3,
    @SerializedName("tipo") val tipo: String? = null,
    @SerializedName("notas") val notas: String? = null
)


data class PuntoGeografico(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double
)

data class ZonaPeligrosaResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("usuario_id") val usuarioId: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("poligono") val poligono: List<PuntoGeografico>,
    @SerializedName("nivel_peligro") val nivelPeligro: Int,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("activa") val activa: Boolean,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    @SerializedName("notas") val notas: String?,
    @SerializedName("radio_metros") val radioMetros: Int?
)

data class RutaParaValidar(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("geometry") val geometry: String,
    @SerializedName("distance") val distance: Double? = null,
    @SerializedName("duration") val duration: Double? = null
)

data class ValidarRutasRequest(
    @SerializedName("rutas") val rutas: List<RutaParaValidar>,
    @SerializedName("ubicacion_id") val ubicacionId: Int
)

data class ZonaDetectada(
    @SerializedName("zona_id") val zonaId: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("nivel_peligro") val nivelPeligro: Int,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("porcentaje_ruta") val porcentajeRuta: Float
)

data class RutaValidada(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("es_segura") val esSegura: Boolean,
    @SerializedName("nivel_riesgo") val nivelRiesgo: Int,
    @SerializedName("zonas_detectadas") val zonasDetectadas: List<ZonaDetectada>,
    @SerializedName("mensaje") val mensaje: String?,
    @SerializedName("distancia") val distancia: Double?,
    @SerializedName("duracion") val duracion: Double?,

    @SerializedName("zonas_publicas_detectadas") val zonasPublicasDetectadas: List<ZonaPublicaDetectada>? = null
)

data class ValidarRutasResponse(
    @SerializedName("rutas_validadas") val rutasValidadas: List<RutaValidada>,
    @SerializedName("tipo_ml_recomendado") val tipoMlRecomendado: String,
    @SerializedName("todas_seguras") val todasSeguras: Boolean,
    @SerializedName("mejor_ruta_segura") val mejorRutaSegura: String?,
    @SerializedName("advertencia_general") val advertenciaGeneral: String?,
    @SerializedName("total_zonas_usuario") val totalZonasUsuario: Int,

    @SerializedName("zonas_publicas_encontradas") val zonasPublicasEncontradas: Int? = null
)

data class ZonaPublicaDetectada(
    @SerializedName("zona_id") val zonaId: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("nivel_peligro") val nivelPeligro: Int,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("distancia_km") val distanciaKm: Float,
    @SerializedName("puede_guardar") val puedeGuardar: Boolean
)