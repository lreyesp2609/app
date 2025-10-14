package com.example.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReminderType {
    LOCATION, DATETIME, BOTH
}

enum class TriggerType {
    ENTER, EXIT, BOTH
}

enum class SoundType {
    DEFAULT, GENTLE, ALERT, CHIME
}

data class Reminder(
    val id: Int? = null,
    val user_id: Int? = null,
    val title: String,
    val description: String? = null,
    val reminder_type: String,
    val trigger_type: String,
    val vibration: Boolean = false,
    val sound: Boolean = false,
    val sound_type: String? = null,
    val days: List<String>? = null,
    val time: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val radius: Double?
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String?,
    val reminder_type: String,
    val trigger_type: String,
    val sound_type: String?,
    val vibration: Boolean,
    val sound: Boolean,
    val days: String?,
    val time: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val radius: Float?,
    val user_id: Int
)