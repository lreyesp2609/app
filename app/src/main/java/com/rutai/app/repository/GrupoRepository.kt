package com.rutai.app.repository

import com.rutai.app.models.GrupoCreate
import com.rutai.app.models.GrupoDeleteResponse
import com.rutai.app.models.GrupoResponse
import com.rutai.app.models.GrupoResponseSalir
import com.rutai.app.models.IntegrantesResponse
import com.rutai.app.network.GrupoService
import retrofit2.Response

class GrupoRepository(private val grupoService: GrupoService) {

    suspend fun createGrupo(token: String, grupoCreate: GrupoCreate): Response<GrupoResponse> {
        val authHeader = "Bearer $token"
        return grupoService.createGrupo(grupoCreate, authHeader)
    }

    suspend fun listarGrupos(token: String): Response<List<GrupoResponse>> {
        val authHeader = "Bearer $token"
        return grupoService.listarGrupos(authHeader)
    }

    suspend fun unirseAGrupo(token: String, codigo: String): Response<GrupoResponse> {
        val authHeader = "Bearer $token"
        return grupoService.unirseAGrupo(codigo, authHeader)
    }

    suspend fun obtenerIntegrantes(token: String, grupoId: Int): Response<IntegrantesResponse> {
        val authHeader = "Bearer $token"
        return grupoService.obtenerIntegrantes(grupoId, authHeader)
    }

    suspend fun salirDelGrupo(token: String, grupoId: Int): Response<GrupoResponseSalir> {
        return grupoService.salirDelGrupo(
            grupoId = grupoId,
            token = "Bearer $token"
        )
    }

    suspend fun eliminarGrupo(token: String, grupoId: Int): Response<GrupoDeleteResponse> {
        return grupoService.eliminarGrupo(grupoId, "Bearer $token")
    }
}
