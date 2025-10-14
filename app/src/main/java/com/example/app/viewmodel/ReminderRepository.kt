package com.example.app.viewmodel

import com.example.app.models.ReminderEntity
import com.example.app.network.ReminderDao

class ReminderRepository(private val dao: ReminderDao) {

    // Obtener todos los recordatorios locales
    suspend fun getLocalReminders(): List<ReminderEntity> = dao.getAllReminders()

    // Obtener un recordatorio por ID
    suspend fun getReminderById(id: Int): ReminderEntity? = dao.getReminderById(id)

    // Guardar un solo recordatorio
    suspend fun saveReminder(reminder: ReminderEntity) {
        dao.insertReminder(reminder)
    }

    // Actualizar un recordatorio existente
    suspend fun updateReminder(reminder: ReminderEntity) {
        dao.updateReminder(reminder)
    }

    // Eliminar un recordatorio
    suspend fun deleteReminder(reminder: ReminderEntity) {
        dao.deleteReminder(reminder)
    }

    // Eliminar un recordatorio por ID
    suspend fun deleteReminderById(id: Int) {
        dao.deleteReminderById(id)
    }
}
