package com.example.app.repository

import com.example.app.models.GrupoCreate
import com.example.app.models.GrupoResponseSalir
import com.example.app.models.IntegrantesResponse
import com.example.app.network.GrupoResponse
import com.example.app.network.GrupoService
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
}
