package com.example.app.network

import androidx.room.*
import com.example.app.models.ReminderEntity

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE is_deleted = 0 AND is_active = 1 ORDER BY id DESC")
    suspend fun getAllReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :reminderId AND is_deleted = 0 LIMIT 1")
    suspend fun getReminderById(reminderId: Int): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE is_deleted = 0 ORDER BY id DESC")
    suspend fun getAllRemindersIncludingInactive(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET is_deleted = 1 WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Int)

    @Query("UPDATE reminders SET is_active = :active WHERE id = :reminderId")
    suspend fun setReminderActive(reminderId: Int, active: Boolean)
}