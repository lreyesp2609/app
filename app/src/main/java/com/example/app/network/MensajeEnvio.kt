package com.example.app.network

data class MensajeEnvioWrapper(
    val action: String = "mensaje",
    val data: MensajeEnvioData
)

data class MensajeEnvioData(
    val contenido: String,
    val tipo: String = "texto"
)