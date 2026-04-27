package com.rutai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class UbicacionesViewModelFactory(private val context: android.content.Context, private val token: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UbicacionesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UbicacionesViewModel(context.applicationContext, token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
