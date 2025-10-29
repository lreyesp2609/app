package com.example.app.network

import com.example.app.models.LoginResponse
import com.example.app.models.User
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @POST("login/")
    suspend fun login(
        @Field("correo") correo: String,
        @Field("contrasenia") contrasenia: String,
        @Field("dispositivo") dispositivo: String? = null,
        @Field("version_app") versionApp: String? = null,
        @Field("ip") ip: String? = null
    ): Response<LoginResponse>

    @GET("login/decodificar")
    suspend fun getCurrentUser(@Header("Authorization") authorization: String): Response<User>

    @FormUrlEncoded
    @POST("login/refresh")
    suspend fun refreshToken(
        @Field("refresh_token") refreshToken: String
    ): Response<LoginResponse>

    @FormUrlEncoded
    @POST("usuarios/registrar")
    suspend fun register(
        @Field("nombre") nombre: String,
        @Field("apellido") apellido: String,
        @Field("correo") correo: String,
        @Field("contrasenia") contrasenia: String
    ): Response<LoginResponse>

    @FormUrlEncoded
    @POST("login/logout/")
    suspend fun logout(
        @Field("refresh_token") refreshToken: String
    ): Response<Unit>

    @POST("/api/fcm/token")
    suspend fun registrarFCMToken(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<Unit>

    @DELETE("/api/fcm/tokens")
    suspend fun eliminarTodosLosTokens(
        @Header("Authorization") token: String
    ): Response<Unit>
}
