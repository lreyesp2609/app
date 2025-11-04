package com.example.app.models

data class IntegranteGrupo(
    val usuario_id: Int,
    val nombre_completo: String,
    val nombre: String,
    val apellido: String,
    val rol: String,
    val activo: Boolean,
    val fecha_union: String,
    val es_creador: Boolean
)

data class IntegrantesResponse(
    val grupo_id: Int,
    val grupo_nombre: String,
    val total_integrantes: Int,
    val integrantes: List<IntegranteGrupo>
)

data class GrupoResponseSalir(
    val id: Int? = null,
    val nombre: String? = null,
    val descripcion: String? = null,
    val message: String? = null
)

data class GrupoDeleteResponse(
    val message: String
)