package com.example.app.viewmodel

import com.example.app.models.ReminderEntity
import com.example.app.network.ReminderDao

class ReminderRepository(private val dao: ReminderDao) {

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
}