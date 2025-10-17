package com.example.app.viewmodel

import com.example.app.models.GrupoCreate
import com.example.app.network.GrupoService
import com.example.app.network.GrupoResponse
import retrofit2.Response

class GrupoRepository(private val grupoService: GrupoService) {

    suspend fun createGrupo(token: String, grupoCreate: GrupoCreate): Response<GrupoResponse> {
        val authHeader = "Bearer $token"
        return grupoService.createGrupo(grupoCreate, authHeader)
    }
}