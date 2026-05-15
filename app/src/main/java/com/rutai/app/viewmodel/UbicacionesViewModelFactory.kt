package com.rutai.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rutai.app.utils.SessionManager

class UbicacionesViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UbicacionesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UbicacionesViewModel(context.applicationContext, SessionManager.getInstance(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
