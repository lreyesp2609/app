package com.rutai.app.models

import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApiService {

    // Endpoint existente para reverse geocoding
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json"
    ): NominatimResponse

    // 🆕 Búsqueda básica de ubicaciones (sin restricción geográfica)
    @GET("search")
    suspend fun searchLocation(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("addressdetails") addressdetails: Int = 1
    ): List<NominatimResponse>

    // 🆕 NUEVO: Búsqueda con viewbox (prioriza resultados cercanos)
    @GET("search")
    suspend fun searchLocationWithViewbox(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("addressdetails") addressdetails: Int = 1,
        @Query("viewbox") viewbox: String, // "left,top,right,bottom"
        @Query("bounded") bounded: Int = 0 // 0 = prioriza, 1 = limita estrictamente
    ): List<NominatimResponse>
}

// Modelo existente
data class NominatimResponse(
    val place_id: Long?,
    val lat: String?,
    val lon: String?,
    val display_name: String?,
    val address: Address?
)

data class Address(
    val road: String?,
    val suburb: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val postcode: String?
)

// 🆕 NUEVO: Modelo para resultados de búsqueda
data class NominatimSearchResult(
    val place_id: Long,
    val lat: String,
    val lon: String,
    val display_name: String,
    val type: String? = null,
    val importance: Double? = null,
    val address: Address? = null
)