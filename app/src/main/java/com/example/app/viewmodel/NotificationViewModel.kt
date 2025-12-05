package com.example.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// NotificationViewModel.kt
class NotificationViewModel : ViewModel() {
    private val _notificationState = MutableStateFlow<NotificationState?>(null)
    val notificationState: StateFlow<NotificationState?> = _notificationState.asStateFlow()

    fun showSuccess(message: String, duration: Long = 3000L) {
        _notificationState.value = NotificationState(
            message = message,
            type = NotificationType.SUCCESS,
            duration = duration
        )
    }

    fun showError(message: String, duration: Long = 4000L) {
        _notificationState.value = NotificationState(
            message = message,
            type = NotificationType.ERROR,
            duration = duration
        )
    }

    fun showInfo(message: String, duration: Long = 3000L) {
        _notificationState.value = NotificationState(
            message = message,
            type = NotificationType.INFO,
            duration = duration
        )
    }

    fun showWarning(message: String, duration: Long = 3500L) {
        _notificationState.value = NotificationState(
            message = message,
            type = NotificationType.WARNING,
            duration = duration
        )
    }

    fun dismiss() {
        _notificationState.value = null
    }
}

data class NotificationState(
    val message: String,
    val type: NotificationType,
    val duration: Long
)

enum class NotificationType {
    SUCCESS, ERROR, INFO, WARNING
}