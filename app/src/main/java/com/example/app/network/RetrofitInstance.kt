package com.example.app.network

import com.example.app.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", BuildConfig.ORS_API_KEY)
                .build()
            chain.proceed(request)
        }
        .build()

    val api: ORSService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ORSService::class.java)
    }
}