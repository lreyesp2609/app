package com.example.app.repository

import com.example.app.models.GrupoCreate
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
}