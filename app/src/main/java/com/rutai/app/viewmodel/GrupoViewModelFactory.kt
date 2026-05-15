package com.rutai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rutai.app.repository.GrupoRepository
import com.rutai.app.utils.SessionManager

class GrupoViewModelFactory(
    private val context: android.content.Context,
    private val repository: GrupoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GrupoViewModel::class.java)) {
            val sessionManager = SessionManager.getInstance(context)
            return GrupoViewModel(context, repository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
