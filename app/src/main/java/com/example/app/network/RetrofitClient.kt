package com.example.app.network

import com.example.app.BuildConfig
import com.example.app.utils.DaysTypeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // ✅ Aumentar timeouts para evitar fallos prematuros
        .connectTimeout(60, TimeUnit.SECONDS)  // Era 30s
        .readTimeout(60, TimeUnit.SECONDS)     // Era 30s
        .writeTimeout(60, TimeUnit.SECONDS)    // Era 30s
        // ✅ Agregar reintentos automáticos
        .retryOnConnectionFailure(true)
        .build()

    private val gson = GsonBuilder()
        .registerTypeAdapter(
            object : TypeToken<List<String>?>() {}.type,
            DaysTypeAdapter()
        )
        .create()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
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

    val grupoService: GrupoService by lazy {
        retrofit.create(GrupoService::class.java)
    }

    val mensajesService: MensajesApiService by lazy {
        retrofit.create(MensajesApiService::class.java)
    }
}