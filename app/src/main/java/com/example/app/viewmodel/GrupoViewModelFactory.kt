package com.example.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.app.repository.GrupoRepository

class GrupoViewModelFactory(
    private val repository: GrupoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GrupoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GrupoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}