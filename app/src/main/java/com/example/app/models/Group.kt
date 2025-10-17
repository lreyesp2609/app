package com.example.app.models

import com.google.gson.annotations.SerializedName

data class GrupoCreate(
    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String? = null
)
