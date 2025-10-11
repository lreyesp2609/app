package com.example.app.viewmodel

import com.example.app.models.ReminderEntity
import com.example.app.network.ReminderDao

class ReminderRepository(private val dao: ReminderDao) {

    // Guardar múltiples recordatorios desde la API
    suspend fun saveRemindersFromApi(reminders: List<ReminderEntity>) {
        dao.clearAll()
        dao.insertReminders(reminders)
    }

    // Obtener todos los recordatorios locales
    suspend fun getLocalReminders(): List<ReminderEntity> = dao.getAllReminders()

    // 🆕 Guardar un solo recordatorio
    suspend fun saveReminder(reminder: ReminderEntity) {
        dao.insertReminder(reminder)
    }

    // 🆕 Eliminar un recordatorio
    suspend fun deleteReminder(reminder: ReminderEntity) {
        dao.deleteReminder(reminder)
    }
}