package com.example.app.network
import com.google.gson.annotations.SerializedName
import retrofit2.http.*

data class VerificarUbicacionRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double
)

data class ZonaPeligrosaDetectada(
    @SerializedName("zona_id") val zona_id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("nivel_peligro") val nivel_peligro: Int,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("distancia_al_centro") val distancia_al_centro: Double,
    @SerializedName("dentro_de_zona") val dentro_de_zona: Boolean
)

data class VerificarUbicacionResponse(
    @SerializedName("hay_peligro") val hay_peligro: Boolean,
    @SerializedName("zonas_detectadas") val zonas_detectadas: List<ZonaPeligrosaDetectada>,
    @SerializedName("mensaje_alerta") val mensaje_alerta: String?
)
interface SeguridadApiService {

    @POST("seguridad/verificar-ubicacion-actual")
    suspend fun verificarUbicacionActual(
        @Header("Authorization") token: String,
        @Body request: VerificarUbicacionRequest
    ): VerificarUbicacionResponse
}