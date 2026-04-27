package com.rutai.app.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.rutai.app.utils.DaysTypeAdapter
import com.rutai.app.BuildConfig
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 🆕 Necesita contexto para el AuthInterceptor
    private lateinit var applicationContext: Context

    /**
     * Debe llamarse desde Application.onCreate()
     */
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    // 🆕 AuthInterceptor para refresh automático
    private val authInterceptor by lazy {
        AuthInterceptor(applicationContext)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = Level.BODY
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            // 🔥 ORDEN CRÍTICO: AuthInterceptor PRIMERO, Logging DESPUÉS
            // Esto permite que Auth cierre/modifique responses antes del logging
            .addInterceptor(authInterceptor)       // ✅ 1º - Maneja 401 y refresh
            .addInterceptor(loggingInterceptor)    // ✅ 2º - Loggea después

            // Timeouts aumentados
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)

            // Reintentos automáticos
            .retryOnConnectionFailure(true)
            .build()
    }

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

    val trackingApiService: TrackingApiService by lazy {
        retrofit.create(TrackingApiService::class.java)
    }

    val seguridadApiService: SeguridadApiService by lazy {
        retrofit.create(SeguridadApiService::class.java)
    }
}