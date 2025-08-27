package com.example.app.models

data class UbicacionUsuarioCreate(
    val nombre: String,
    val latitud: Double,
    val longitud: Double,
    val direccion_completa: String
)

data class UbicacionUsuarioResponse(
    val id: Int,
    val usuario_id: Int,
    val nombre: String,
    val latitud: Double,
    val longitud: Double,
    val direccion_completa: String
)
