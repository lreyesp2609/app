package com.rutai.app.utils

import retrofit2.Response
import org.json.JSONObject

/**
 * 🛠️ Utilidad para envolver llamadas de Retrofit en Result<T>.
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.success(body)
            } else {
                // Si el cuerpo es null pero la respuesta fue exitosa (ej: 204 No Content)
                // Intentamos un casting arriesgado o devolvemos éxito con null si T lo permite.
                @Suppress("UNCHECKED_CAST")
                Result.success(Unit as T)
            }
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMessage = try {
                JSONObject(errorBody ?: "{}").optString("detail", "Error desconocido")
            } catch (e: Exception) {
                errorBody ?: "Error desconocido"
            }
            Result.failure(Exception(errorMessage))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
