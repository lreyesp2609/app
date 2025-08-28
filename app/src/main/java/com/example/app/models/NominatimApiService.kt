package com.example.app.models

import retrofit2.http.GET
import retrofit2.http.Query

data class NominatimResponse(
    val display_name: String?,
    val lat: String?,
    val lon: String?
)

// Actualizar NominatimApiService para incluir búsqueda
interface NominatimApiService {
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("format") format: String = "json",
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("addressdetails") addressdetails: Int = 1
    ): NominatimResponse

    // Nuevo método para búsqueda de ubicaciones (geocoding)
    @GET("search")
    suspend fun searchLocation(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("addressdetails") addressdetails: Int = 1
    ): List<NominatimResponse>
}