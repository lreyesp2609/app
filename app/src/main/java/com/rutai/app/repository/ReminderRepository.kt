package com.rutai.app.repository

import com.rutai.app.models.ReminderEntity
import com.rutai.app.models.ReminderRequest
import com.rutai.app.models.ReminderResponse
import com.rutai.app.network.ReminderApiService
import com.rutai.app.network.ReminderDao
import com.rutai.app.network.RetrofitClient
import com.rutai.app.utils.safeApiCall

class ReminderRepository(private val dao: ReminderDao) {

    private val apiService: ReminderApiService = RetrofitClient.reminderService

    suspend fun getLocalReminders(): List<ReminderEntity> = dao.getAllReminders()

    suspend fun getReminderById(id: Int): ReminderEntity? = dao.getReminderById(id)

    suspend fun getAllRemindersForLocationService(): List<ReminderEntity> =
        dao.getAllRemindersIncludingInactive()

    suspend fun saveReminder(reminder: ReminderEntity) {
        dao.insertReminder(reminder) // Por defecto is_active=true, is_deleted=false
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        dao.updateReminder(reminder)
    }

    // Eliminación
    suspend fun deleteReminderById(id: Int) {
        dao.deleteReminderById(id)
    }

    // Habilitar/deshabilitar
    suspend fun setReminderActive(reminderId: Int, active: Boolean) {
        dao.setReminderActive(reminderId, active)
    }

    // En ReminderRepository
    suspend fun clearAllReminders() {
        dao.clearAllReminders()
    }

    // API calls
    suspend fun fetchReminders(token: String): Result<List<ReminderResponse>> {
        return safeApiCall { apiService.getReminders("Bearer $token") }
    }

    suspend fun createReminder(token: String, request: ReminderRequest): Result<ReminderResponse> {
        return safeApiCall { apiService.createReminder("Bearer $token", request) }
    }

    suspend fun toggleReminder(token: String, reminderId: Int): Result<ReminderResponse> {
        return safeApiCall { apiService.toggleReminder("Bearer $token", reminderId) }
    }

    suspend fun deleteReminder(token: String, reminderId: Int): Result<Unit> {
        return safeApiCall { apiService.deleteReminder("Bearer $token", reminderId) }
    }

    suspend fun updateReminderApi(token: String, reminderId: Int, request: ReminderRequest): Result<ReminderResponse> {
        return safeApiCall { apiService.updateReminder("Bearer $token", reminderId, request) }
    }
}
