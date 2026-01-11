package com.example.app.models

import android.util.Log
import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    val routes: List<Route>,
    val profile: String
)

data class Route(
    val summary: Summary,
    val geometry: String,
    val segments: List<Segment>
)

data class Summary(
    val distance: Double, // en metros
    val duration: Double  // en segundos
)

data class Segment(
    val distance: Double,
    val duration: Double,
    val steps: List<Step>
)

data class Step(
    val instruction: String,
    val distance: Double,
    val duration: Double,
    val type: Int = mapInstructionToType(instruction)
)

data class DirectionsRequest(
    val coordinates: List<List<Double>>,
    val language: String = "es",
    val units: String = "km",
    val instructions: Boolean = true,
    val preference: String = "fastest",
    val options: DirectionsOptions? = null  // 🔥 CAMBIO: de AvoidOptions a DirectionsOptions
)

data class DirectionsOptions(
    @SerializedName("avoid_polygons")
    val avoid_polygons: Map<String, Any>? = null,  // ✅ Map en lugar de List

    @SerializedName("avoid_features")
    val avoid_features: List<String>? = null
)

// función helper para mapear la instrucción a un tipo
fun mapInstructionToType(instruction: String): Int {
    val text = instruction.lowercase()
    return when {
        "gire a la derecha" in text -> 1
        "gire a la izquierda" in text -> 0
        "siga recto" in text -> 2
        "arrive" in text || "llegar" in text -> 10
        "head" in text -> 11
        else -> -1 // tipo desconocido
    }
}
fun DirectionsResponse.toRutaUsuarioJson(
    ubicacionId: Int,
    transporteTexto: String,
    tipoRutaUsado: String? = null
): RutaUsuario {
    val ruta = this.routes.firstOrNull()
    return RutaUsuario(
        transporte_texto = transporteTexto,
        ubicacion_id = ubicacionId,
        distancia_total = ruta?.summary?.distance ?: 0.0,
        duracion_total = ruta?.summary?.duration ?: 0.0,
        geometria = ruta?.geometry ?: "",
        fecha_inicio = System.currentTimeMillis().toLocalISOString(), // 🔥 CAMBIO
        segmentos = ruta?.segments?.map { segment ->
            SegmentoRuta(
                distancia = segment.distance,
                duracion = segment.duration,
                pasos = segment.steps.map { step ->
                    PasoRuta(
                        instruccion = step.instruction,
                        distancia = step.distance,
                        duracion = step.duration,
                        tipo = step.type
                    )
                }
            )
        } ?: emptyList(),
        tipo_ruta_usado = tipoRutaUsado
    )
}

// Helper para convertir milisegundos a timezone local
fun Long.toLocalISOString(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.getDefault())
    val result = sdf.format(java.util.Date(this))
    Log.d("TimezoneDebug", "Emulador timezone: ${java.util.TimeZone.getDefault().id}")
    Log.d("TimezoneDebug", "Fecha generada: $result")
    return result
}


// En tu data class RouteAlternative
data class RouteAlternative(
    val type: String,
    val displayName: String,
    val response: DirectionsResponse,
    val distance: Double,
    val duration: Double,
    val isRecommended: Boolean,
    val esSegura: Boolean? = null,  // ← Puede ser null
    val nivelRiesgo: Int? = null,
    val zonasDetectadas: List<ZonaDetectada>? = null,
    val mensajeSeguridad: String? = null,

    val publicZonesDetected: List<ZonaPublicaDetectada>? = null,

    val zonasPublicasDetectadas: List<ZonaPublicaDetectada>? = null

) {
    // 🔥 AGREGAR ESTA FUNCIÓN DE DEBUG
    fun logInfo(tag: String) {
        Log.d(tag, "RouteAlternative:")
        Log.d(tag, "  tipo: $type")
        Log.d(tag, "  esSegura: $esSegura")
        Log.d(tag, "  nivelRiesgo: $nivelRiesgo")
        Log.d(tag, "  zonas: ${zonasDetectadas?.size ?: 0}")
    }
}


// 🔥 DATA CLASS PARA LAS ZONAS GUARDADAS
data class ZonaGuardada(
    val lat: Double,
    val lon: Double,
    val radio: Int,
    val nombre: String,
    val nivel: Int,
    val id: Int? = null
)

