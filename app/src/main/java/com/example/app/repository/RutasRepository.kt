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

    suspend fun listarRutas(token: String): Result<List<RutaUsuario>> {
        return try {
            val response = api.listarRutas("Bearer $token")
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Lista vacía"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error desconocido"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Error HTTP: ${e.message}"))
        }
    }

    suspend fun obtenerRutaPorId(token: String, id: Int): Result<RutaUsuario> {
        return try {
            val response = api.obtenerRuta("Bearer $token", id)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Ruta no encontrada"))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error desconocido"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Error HTTP: ${e.message}"))
        }
    }

    suspend fun eliminarRuta(token: String, id: Int): Result<Unit> {
        return try {
            val response = api.eliminarRuta("Bearer $token", id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error desconocido"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Error de red: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Error HTTP: ${e.message}"))
        }
    }
}
