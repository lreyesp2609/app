package com.example.app.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.app.BuildConfig

object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val ubicacionesApiService: UbicacionesApiService by lazy {
        retrofit.create(UbicacionesApiService::class.java)
    }

    val rutasApiService: RutasApiService by lazy {
        retrofit.create(RutasApiService::class.java)
    }

    val mlService: MLService by lazy {
        retrofit.create(MLService::class.java)
    }

    val reminderService: ReminderApiService by lazy {
        retrofit.create(ReminderApiService::class.java)
    }
}