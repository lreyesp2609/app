package com.example.app.models

data class TipoRutaResponse(
    val tipo_ruta: String,
    val usuario_id: Int,
    val confidence: Double? = null
)

data class FeedbackRequest(
    val tipo_usado: String,
    val completada: Boolean,
    val distancia: Double? = null,
    val duracion: Double? = null
)

data class FeedbackResponse(
    val status: String,
    val mensaje: String
)

data class EstadisticasResponse(
    val usuario_id: Int,
    val bandits: List<BanditStats>,
    val total_rutas_generadas: Int
)

data class BanditStats(
    val tipo_ruta: String,
    val total_usos: Int,
    val total_rewards: Int,
    val success_rate: Double,
    val ucb_score: Double?
)