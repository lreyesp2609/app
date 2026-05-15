package com.rutai.app.repository


import android.content.Context
import com.rutai.app.models.MarcarLeidoResponse
import com.rutai.app.models.MensajeResponse
import com.rutai.app.network.MensajesApiService
import com.rutai.app.network.RetrofitClient
import com.rutai.app.utils.SessionManager
import com.rutai.app.utils.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MensajesRepository(private val context: Context) {

    private val apiService: MensajesApiService = RetrofitClient.mensajesService
    private val sessionManager = SessionManager.getInstance(context)

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
        token: String,
        grupoId: Int,
        limit: Int = 50
    ): Result<List<MensajeResponse>> = withContext(Dispatchers.IO) {
        safeApiCall {
            apiService.obtenerMensajesGrupo(
                grupoId = grupoId,
                limit = limit,
                token = "Bearer $token"
            )
        }
    }

    /**
     * Marca un mensaje como leído
     */
    suspend fun marcarMensajeLeido(
        token: String,
        grupoId: Int,
        mensajeId: Int
    ): Result<MarcarLeidoResponse> = withContext(Dispatchers.IO) {
        safeApiCall {
            apiService.marcarMensajeLeido(
                grupoId = grupoId,
                mensajeId = mensajeId,
                token = "Bearer $token"
            )
        }
    }
}
