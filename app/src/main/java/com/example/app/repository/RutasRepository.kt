package com.example.app.repository

import com.example.app.models.RutaUsuario
import com.example.app.network.RetrofitClient
import retrofit2.HttpException
import java.io.IOException

class RutasRepository {
    private val api = RetrofitClient.rutasApiService

    suspend fun guardarRuta(token: String, ruta: RutaUsuario): Result<RutaUsuario> {
        return try {
            val response = api.crearRuta("Bearer $token", ruta)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Respuesta vacía"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error desconocido"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Error HTTP: ${e.message}"))
        }
    }
    suspend fun cancelarRuta(rutaId: Int) {
        api.cancelarRuta(rutaId)
    }

    suspend fun finalizarRuta(rutaId: Int) {
        api.finalizarRuta(rutaId)
    }
}
