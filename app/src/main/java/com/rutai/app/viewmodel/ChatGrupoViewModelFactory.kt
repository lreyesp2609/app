package com.rutai.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.rutai.app.repository.MensajesRepository
import com.rutai.app.utils.SessionManager

class ChatGrupoViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatGrupoViewModel::class.java)) {
            val sessionManager = SessionManager.getInstance(context)
            val repository = MensajesRepository(context)
            return ChatGrupoViewModel(context, repository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}