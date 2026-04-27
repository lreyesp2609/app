package com.rutai.app.models

data class ZonaSugerida(
    val zonaOriginal: ZonaPeligrosaResponse,
    val distanciaKm: Float,
    val yaAdoptada: Boolean = false
)