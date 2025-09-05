package com.example.app.models

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
    val preference: String = "fastest", // Nuevo: para ML
    val options: AvoidOptions? = null   // Nuevo: para rutas scenic
)

data class AvoidOptions(
    val avoid_features: List<String> = emptyList()
)

// Helper para convertir tipo ML a configuración OpenRouteService
fun String.toORSConfig(): Pair<String, AvoidOptions?> {
    return when (this) {
        "fastest" -> "fastest" to null
        "shortest" -> "shortest" to null
        "scenic" -> "recommended" to AvoidOptions(listOf("highways", "tollways"))
        "balanced" -> "recommended" to null
        else -> "fastest" to null
    }
}

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

// En tu archivo de modelos DirectionsResponse, actualiza esta función:

fun DirectionsResponse.toRutaUsuarioJson(
    ubicacionId: Int,
    transporteTexto: String
): RutaUsuario {
    val ruta = this.routes.firstOrNull()
    return RutaUsuario(
        transporte_texto = transporteTexto,
        ubicacion_id = ubicacionId,  // ✅ Está aquí
        distancia_total = ruta?.summary?.distance ?: 0.0,
        duracion_total = ruta?.summary?.duration ?: 0.0,
        geometria = ruta?.geometry ?: "",
        fecha_inicio = System.currentTimeMillis().toString(),
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
        } ?: emptyList()
    )
}

// Helper para convertir milisegundos a ISO 8601
fun Long.toISOString(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return sdf.format(java.util.Date(this))
}
