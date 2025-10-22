package com.example.app.repository


import android.content.Context
import com.example.app.models.MarcarLeidoResponse
import com.example.app.models.MensajeResponse
import com.example.app.network.MensajesApiService
import com.example.app.network.RetrofitClient
import com.example.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MensajesRepository(private val context: Context) {

    private val apiService: MensajesApiService = RetrofitClient.mensajesService
    private val sessionManager = SessionManager.getInstance(context)

    /**
     * Obtiene el token de acceso guardado
     */
    private fun getAuthHeader(): String {
        val token = sessionManager.getAccessToken()
        return "Bearer $token"
    }

    /**
     * Obtiene el ID del usuario actual desde SessionManager
     */
    fun getCurrentUserId(): Int {
        return sessionManager.getUser()?.id ?: -1
    }

    /**
     * Obtiene los mensajes de un grupo
     */
    suspend fun obtenerMensajesGrupo(
        grupoId: Int,
        limit: Int = 50
    ): Result<List<MensajeResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerMensajesGrupo(
                grupoId = grupoId,
                limit = limit,
                token = getAuthHeader() // ← Aquí el token correcto
            )

            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marca un mensaje como leído
     */
    suspend fun marcarMensajeLeido(
        grupoId: Int,
        mensajeId: Int
    ): Result<MarcarLeidoResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.marcarMensajeLeido(
                grupoId = grupoId,
                mensajeId = mensajeId,
                token = getAuthHeader()
            )

            if (response.isSuccessful) {
                Result.success(response.body() ?: MarcarLeidoResponse("Error", false))
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
