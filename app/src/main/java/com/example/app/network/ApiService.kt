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
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<LoginResponse>
}
