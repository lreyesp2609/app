package com.example.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class UbicacionesViewModelFactory(private val token: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UbicacionesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UbicacionesViewModel(token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
