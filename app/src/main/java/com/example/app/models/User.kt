package com.example.app.models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String
)


data class User(
    @SerializedName("id")
    val id: Int,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("apellido")
    val apellido: String,
    @SerializedName("activo")
    val activo: Boolean,
    @SerializedName("id_rol")
    val idRol: Int,
    @SerializedName("rol")
    val rol: String
)