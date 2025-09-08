package com.example.app.models

data class TipoRutaRequest(
    val ubicacion_id: Int
)

data class TipoRutaResponse(
    val tipo_ruta: String,
    val usuario_id: Int,
    val ubicacion_id: Int,
    val confidence: Double? = null
)

data class FeedbackRequest(
    val tipo_usado: String,
    val completada: Boolean,
    val ubicacion_id: Int,
    val distancia: Double? = null,
    val duracion: Double? = null
)

data class FeedbackResponse(
    val status: String,
    val mensaje: String
)

data class EstadisticasResponse(
    val usuario_id: Int,
    val ubicacion_id: Int? = null,
    val bandits: List<BanditStats>,
    val total_rutas_generadas: Int,
    val rutas_completadas: Int = 0,
    val rutas_canceladas: Int = 0,
    val tiempo_promedio_por_tipo: Map<String, Double> = emptyMap()
)

data class BanditStats(
    val tipo_ruta: String,
    val total_usos: Int,
    val total_rewards: Int,
    val success_rate: Double,
    val ucb_score: Double?,
    val fecha_creacion: String,
    val fecha_actualizacion: String
)

