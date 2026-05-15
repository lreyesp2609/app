package com.rutai.app.repository

import com.rutai.app.models.GrupoCreate
import com.rutai.app.models.GrupoDeleteResponse
import com.rutai.app.models.GrupoResponse
import com.rutai.app.models.GrupoResponseSalir
import com.rutai.app.models.IntegrantesResponse
import com.rutai.app.network.GrupoService
import com.rutai.app.utils.safeApiCall

class GrupoRepository(private val grupoService: GrupoService) {

    suspend fun createGrupo(token: String, grupoCreate: GrupoCreate): Result<GrupoResponse> {
        return safeApiCall { grupoService.createGrupo(grupoCreate, "Bearer $token") }
    }

    suspend fun listarGrupos(token: String): Result<List<GrupoResponse>> {
        return safeApiCall { grupoService.listarGrupos("Bearer $token") }
    }

    suspend fun unirseAGrupo(token: String, codigo: String): Result<GrupoResponse> {
        return safeApiCall { grupoService.unirseAGrupo(codigo, "Bearer $token") }
    }

    suspend fun obtenerIntegrantes(token: String, grupoId: Int): Result<IntegrantesResponse> {
        return safeApiCall { grupoService.obtenerIntegrantes(grupoId, "Bearer $token") }
    }

    suspend fun salirDelGrupo(token: String, grupoId: Int): Result<GrupoResponseSalir> {
        return safeApiCall { grupoService.salirDelGrupo(grupoId, "Bearer $token") }
    }

    suspend fun eliminarGrupo(token: String, grupoId: Int): Result<GrupoDeleteResponse> {
        return safeApiCall { grupoService.eliminarGrupo(grupoId, "Bearer $token") }
    }
}
